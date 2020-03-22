package com.eventswarm.social.channels;

import com.eventswarm.AddEventAction;
import com.eventswarm.AddEventTrigger;
import com.eventswarm.channels.HttpContentHandler;
import com.eventswarm.events.Event;
import com.eventswarm.events.Source;
import com.eventswarm.events.Sources;
import com.eventswarm.events.jdo.JdoHeader;
import com.eventswarm.eventset.DiscreteTimeWindow;
import com.eventswarm.eventset.EventSet;
import com.eventswarm.eventset.DuplicateFilter;
import com.eventswarm.social.events.JSoupFragmentEvent;
import com.eventswarm.util.Interval;
import com.eventswarm.util.IntervalUnit;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;


/**
 * Class to connect to SuperFeedr and generate events from each HTML fragment matching a CSS selector retrieved
 * from a specified URL.
 *
 * Because SuperFeedr sends us *all* elements matching a given CSS selector whenever *any* element changes or is
 * added, we want to pass on only the new/updated elements. We do this through maintaining a dupeFilter EventSet and using
 * the built-in EventSwarm duplicate detection to discard duplicate content. Thus downstream AddEventActions will
 * actually be connected to the dupeFilter eventset. By default, this EventSet is a 1 week time window, meaning
 * we detect duplicates for content received in the last week. You can provide an alternate EventSet if you need
 * a different window, and that EventSet can be prepopulated with Events you have already seen if you wish.
 *
 * Be aware that the default timestamps associated with such events are based on when SuperFeedr actually
 * sends them to us, not when the content was added to the page at the URL. So if you use a 1 week window and your
 * application runs for more than a week, old content will be delivered again at the end of the week if it's still
 * on the monitored page. You have a few ways to manage this:
 *
 * <ol>
 *     <li>Provide a bigger window that reflects the length of time content stays on the page or the maximum number
 *     of items maintained on the page (i.e. using a LastNWindow).</li>
 *     <li>Use a non-sliding window. Use this for relatively static content with infrequent change. Watch
 *     memory usage!</li>
 *     <li>If your HTML fragments contain a timestamp, create a subclass and override the getTimestamp() method
 *     to parse the timestamp within the fragment. This, combined with a suitable time window, is the preferred
 *     approach.</li>
 * </ol>
 *
 * SuperFeedr polls web html content every 15 minutes, in case that's important to you. There are some
 * smarts on the hub to work out the best polling interval for a URL, but it's not clear if this works on web feeds.
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class SuperFeedrHtmlChannel implements HttpContentHandler, AddEventTrigger {
    private static enum ContentType {xml, json, html}
    private static Pattern JSON_START = Pattern.compile("^\\s*\\{");
    private static Pattern XML_START = Pattern.compile("^\\s*<\\?xml");

    private SuperFeedrSubscriber subscriber;
    private Map<String,URL> subscriptions;
    private Map<String,String> selectors;
    private DuplicateFilter<String> dupeFilter;
    private long count;
    private long errors;
    private MessageDigest digest;


    public static final String DEFAULT_SOURCE = "superfeedr.com";
    public static final String DIGEST_ALG = "SHA1";
    public static final SimpleDateFormat HTTP_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyy HH:mm:ss zzzz");
    public static final String DATE_HEADER = "Date";
    public static final Interval DEFAULT_WINDOW = new Interval(1, IntervalUnit.WEEKS);

    private static final Logger logger = Logger.getLogger(SuperFeedrHtmlChannel.class);

    /**
     * Create a new SuperFeedr channel attached to the specified PubSubHubbubSubscriber instance, which we will
     * assume is connected to SuperFeedr, and create a 1 week time window to hold dupeFilter (i.e. filter dupes)
     *
     * @param subscriber
     */
    public SuperFeedrHtmlChannel(SuperFeedrSubscriber subscriber) {
        this(subscriber, new DiscreteTimeWindow(DEFAULT_WINDOW));
    }

    /**
     * Create a new SuperFeedr channel attached to the specified PubSubHubbubSubscriber instance, which we will
     * assume is connected to SuperFeedr.
     *
     * @param subscriber
     */
    public SuperFeedrHtmlChannel(SuperFeedrSubscriber subscriber, EventSet history) {
        this.subscriber = subscriber;
        this.dupeFilter = new DuplicateFilter<String>(Event.ID_RETRIEVER, history);
        this.subscriptions = new HashMap<String,URL>();
        this.selectors = new HashMap<String,String>();
        this.count = 0;
        this.errors = 0;
        try {
            digest = MessageDigest.getInstance(DIGEST_ALG);
        } catch (NoSuchAlgorithmException exc) {
            // should never get here
            logger.fatal("WTF!!? Cannot use " + DIGEST_ALG + " digest algorithm", exc);
        }
    }

    public PubSubHubbubSubscriber getSubscriber() {
        return subscriber;
    }

    public Map<String, URL> getSubscriptions() {
        return subscriptions;
    }

    public Map<String, String> getSelectors() {
        return selectors;
    }

    public EventSet getHistory() {
        return dupeFilter.getWindow();
    }

    /**
     * Return the duplicate filter used by this channel
     *
     * The duplicate filter has a DuplicateEventTrigger to which you can listen if you want to know about
     * duplicates.
     *
     * @return
     */
    public DuplicateFilter<String> getDupeFilter() {
        return dupeFilter;
    }

    /**
     *
     * @return number of events created by this channel
     */
    public long getCount() {
        return count;
    }

    /**
     *
     * @return number of errors encountered by this channel
     */
    public long getErrors() {
        return errors;
    }

    /**
     * Subscribe this channel to the specified topic URL and web fragment selector via the
     * PubSubHubbubSubscriber provider
     *
     * @param topic URL to subscribe to
     * @param selector CSS selector for the item or items to monitor
     * @param backfill if true, retrieves recent items from the topic and sends them to the handler
     */
    public void subscribe(URL topic, String selector, boolean backfill) throws PubSubHubbubSubscriber.PubSubException {
        String id;
        if (backfill) {
            id = subscriber.subscribeAndRetrieve(this, topic, null);
        } else {
            id = subscriber.subscribe(this, topic);
        }
        subscriptions.put(id, topic);
        selectors.put(id, selector);
    }

    /**
     * Unsubscribe all topics from SuperFeedr
     */
    public void unsubscribe() throws PubSubHubbubSubscriber.PubSubException {
        for (String id : subscriptions.keySet()) {
            subscriber.unsubscribe(id, this);
        }
        subscriptions.clear();
    }

    /**
     * As per the interface contract, extract events from the notification and pass them onwards to downstream
     * actions.
     *
     * @param subs_id
     * @param body InputStream for reading the HTTP request body
     * @param headers Map of headers
     */
    public void handle(String subs_id, InputStream body, Map<String, List<String>> headers) {
        if (!subscriptions.containsKey(subs_id)) {
            logger.warn("Received content for unrecognised subscription: " + subs_id);
        }
        try {
            String content = PubSubHubbubSubscriber.readStream(body);
            ContentType type = getType(content);
            logger.debug("Received content:"); logger.debug(content);
            if (type.equals(ContentType.html)) {
                Document doc = Jsoup.parseBodyFragment(content);
                logger.debug("Received HTML: " + prettyPrint(doc)); // very expensive, so comment out when not required
                Elements elements = doc.select(selectors.get(subs_id));
                for (Element element : elements) {
                    Event event = makeEvent(subs_id, element, headers);
                    if (event != null) {
                        logger.debug("Attempting to add event with id " + event.getHeader().getEventId());
                        dupeFilter.execute(this, event);
                    }
                }
            } else {
                logger.info("Received " + type.toString() + " content, assuming a status object and ignoring");
                logger.debug(content);
            }
        } catch (IOException exc) {
            logger.error("Error reading feed body", exc);
            errors++;
        }
    }

    /**
     * Determine if this is a JSON or ATOM (XML) status feed
     *
     * We make the assumption that anything that isn't JSON or XML is some form of HTML. The patterns used
     * ignore leading spaces in the string.
     *
     * @return json if content matches the JSON_START pattern, xml if content matches the XML_START pattern,
     * html otherwise
     */
    private ContentType getType(String content) {
        if (JSON_START.matcher(content).find()) {
            return ContentType.json;
        } else if (XML_START.matcher(content).find()){
            return ContentType.xml;
        } else {
            // assume HTML otherwise
            return ContentType.html;
        }
    }

    /**
     * Method to create an event from an element and headers
     *
     * @param element HTML Element containing content
     * @param headers Headers of received request
     * @return New event or null if event could not be created
     */
    protected Event makeEvent(String subs_id, Element element, Map<String, List<String>> headers) {
        String id = getId(subs_id, element, headers);
        Date timestamp = getTimestamp(subs_id, element, headers);
        Source source = getSource(subs_id, element, headers);
        if (id == null || timestamp == null || source == null) {
            errors++;
            return null;
        } else {
            count++;
            return new JSoupFragmentEvent(new JdoHeader(timestamp, source, id), element);
        }
    }

    /**
     * Method to create an id from an element and HTTP headers.
     *
     * Default behaviour is to concatenate the topic URL and a "?h=contentHash" parameter
     *
     * @param subs_id
     * @param element
     * @param headers
     * @return event id for this element
     */
    protected String getId(String subs_id, Element element, Map<String, List<String>> headers) {
        return subscriptions.get(subs_id).toString() + "?h=" + contentHash(element);
    }

    /**
     * Method to generate a hash from the content of an element so that we can detect new vs old
     */
    protected String contentHash(Element element) {
        return DatatypeConverter.printBase64Binary(digest.digest(element.text().getBytes()));
    }

    /**
     * Method to extract a suitable timestamp from an element and the HTTP headers
     *
     * Default behaviour is to use the HTTP Date header, or <code>new Date()</code> if a parse error occcurs.
     *
     * @param subs_id
     * @param element
     * @param headers
     * @return timestamp for this element
     */
    protected Date getTimestamp(String subs_id, Element element, Map<String, List<String>> headers) {
        try {
            return HTTP_DATE_FORMAT.parse(headers.get(DATE_HEADER).get(0));
        } catch (ParseException exc) {
            logger.warn("Error parsing HTTP request date", exc);
            return new Date();
        }
    }

    /**
     * Method to extract a source object from an element and the HTTP headers
     *
     * Default behaviour is to return the superfeedr source id (domain name), since the creator of the event is
     * SuperFeedr.
     *
     * @param subs_id
     * @param element
     * @param headers
     * @return source object for this element
     */
    protected Source getSource(String subs_id, Element element, Map<String, List<String>> headers) {
        return Sources.cache.getSourceByName(DEFAULT_SOURCE);
    }


    public void registerAction(AddEventAction action) {
        dupeFilter.registerAction(action);
    }

    public void unregisterAction(AddEventAction action) {
        dupeFilter.unregisterAction(action);
    }

    public static String prettyPrint(Element element) {
        return element.outerHtml();
    }
}
