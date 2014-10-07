package com.eventswarm.social.channels;

import com.eventswarm.AddEventAction;
import com.eventswarm.AddEventTrigger;
import com.eventswarm.events.Event;
import com.eventswarm.events.Header;
import com.eventswarm.events.Source;
import com.eventswarm.events.Sources;
import com.eventswarm.events.jdo.JdoHeader;
import com.eventswarm.events.jdo.OrgJsonEvent;
import com.eventswarm.events.jdo.XmlEventImpl;
import com.eventswarm.util.EventTriggerDelegate;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import javax.xml.xpath.*;


/**
 * Class to connect to SuperFeedr and generate events from each item in received notifications from the hub
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class SuperFeedrAtomChannel implements PubSubContentHandler, AddEventTrigger {
    private EventTriggerDelegate delegate;
    private SuperFeedrSubscriber subscriber;
    private Map<String,URL> subscriptions;
    private DocumentBuilder builder;
    private XPath xpath;
    private XPathExpression entryPath;
    private XPathExpression statusPath;
    private long count;
    private long errors;


    public static final String DEFAULT_SOURCE = "superfeedr.com";
    public static final String FEED_PATH = "/feed";
    public static final String ATOM_ENTRY_PATH = FEED_PATH + "/entry";
    public static final String STATUS_PATH = FEED_PATH + "/status";

    private static final Logger logger = Logger.getLogger(SuperFeedrAtomChannel.class);

    /**
     * Create a new SuperFeedr channel attached to the specified PubSubHubbubSubscriber instance, which we will
     * assume is connected to SuperFeedr.
     *
     * @param subscriber
     */
    public SuperFeedrAtomChannel(SuperFeedrSubscriber subscriber) {
        subscriber.getOptions().put(SuperFeedrSubscriber.PROPS_FORMAT, "atom");
        this.subscriber = subscriber;
        this.delegate = new EventTriggerDelegate(this);
        this.subscriptions = new HashMap<String,URL>();
        makeBuilder();
        xpath = XPathFactory.newInstance().newXPath();
        this.count = 0;
        this.errors = 0;
    }

    private XPathExpression getEntryPath() {
        if (entryPath == null) {
            try {
                entryPath = xpath.compile(ATOM_ENTRY_PATH);
            } catch (XPathExpressionException exc) {
                logger.fatal("WTF!!? Could not parse xpath");
            }
        }
        return entryPath;
    }

    private XPathExpression getStatusPath() {
        if (statusPath == null) {
            try {
                statusPath = xpath.compile(STATUS_PATH);
            } catch (XPathExpressionException exc) {
                logger.fatal("WTF!!? Could not parse xpath");
            }
        }
        return statusPath;
    }

    public PubSubHubbubSubscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(SuperFeedrSubscriber subscriber) {
        this.subscriber = subscriber;
    }

    public Map<String, URL> getSubscriptions() {
        return subscriptions;
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
     * Create a builder for XML documents parsed by this channel
     */
    private void makeBuilder() {
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException exc) {
            logger.error("Failed to create XML document builder", exc);
        }
    }

    /**
     * Subscribe this channel to the specified topic URL via the PubSubHubbubSubscriber provider
     *
     * @param topic URL to subscribe to
     * @param backfill if true, retrieves recent items from the topic and sends them to the handler
     */
    public void subscribe(URL topic, boolean backfill) throws PubSubHubbubSubscriber.PubSubException {
        String id;
        if (backfill) {
            id = subscriber.subscribeAndRetrieve(this, topic, null);
        } else {
            id = subscriber.subscribe(this, topic);
        }
        subscriptions.put(id, topic);
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
     * @param body InputStream for reading the HTTP request body
     * @param headers Map of headers
     */
    public void handle(InputStream body, Map<String, List<String>> headers) {
        try {
            Document doc = builder.parse(body);
            //logger.debug("Received doc: " + prettyPrint(doc, doc)); // very expensive, so comment out when not required
            Node status = (Node) getStatusPath().evaluate(doc, XPathConstants.NODE);
            NodeList nodes = (NodeList) getEntryPath().evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Event event = makeEvent(nodes.item(i), status);
                if (event != null) {
                    delegate.fire(event);
                }
            }
        } catch (IOException exc) {
            logger.error("Error reading feed body", exc);
            errors++;
        } catch (SAXException exc) {
            logger.error("Error parsing feed body", exc);
            errors++;
        } catch (XPathExpressionException exc) {
            logger.error("Error evaluating entry or status path", exc);
            errors++;
        }
    }

    /**
     * Method to create an event from an item node in the Atom feed.
     *
     * @param item
     * @param status
     * @return New event or null if event could not be created
     */
    protected Event makeEvent(Node item, Node status) {
        try {
            String id = getId(item, status);
            Date timestamp = getTimestamp(item, status);
            Source source = getSource(item, status);
            count++;
            return new XmlEventImpl(new JdoHeader(timestamp, source, id), item);
        } catch (XPathExpressionException exc) {
            logger.error("Xpath expression error: ", exc);
            errors++;
            return null;
        } catch (MalformedURLException exc) {
            logger.error("Bad feed href: ", exc);
            errors++;
            return null;
        }
    }

    /**
     * Method to create an id from an item node and/or the status node of the document.
     *
     * Default behaviour is to extract the 'id' element of the entry. Subclasses should override if this is not
     * appropriate.
     *
     * @param item
     * @param status
     * @return id for this item
     * @throws XPathExpressionException
     */
    protected String getId(Node item, Node status) throws XPathExpressionException {
        return xpath.evaluate("id", item);
    }

    /**
     * Method to extract a suitable timestamp from an item node and/or the status node of the document
     *
     * Default behaviour is to extract the 'published' element of the entry and parse using the
     * javax.xml.bind.DatatypeConverter. Subclasses should override if this is not appropriate.
     *
     * @param item
     * @param status
     * @return timestamp for this item
     * @throws XPathExpressionException
     */
    protected Date getTimestamp(Node item, Node status) throws XPathExpressionException {
        return DatatypeConverter.parseDateTime(xpath.evaluate("published", item)).getTime();
    }

    /**
     * Method to extract a suitable timestamp from an item node and/or the status node of the document
     *
     * Default behaviour is to extract the '@feed' attribute of the status (a URL) and extract the domain name
     * from the feed url. Subclasses should override if this is not appropriate.
     *
     * @param item
     * @param status
     */
    protected Source getSource(Node item, Node status) throws XPathExpressionException, MalformedURLException {
        return Sources.cache.getSourceByName(new URL(xpath.evaluate("@feed", status)).getHost());
    }


    @Override
    public void registerAction(AddEventAction action) {
        delegate.registerAction(action);
    }

    @Override
    public void unregisterAction(AddEventAction action) {
        delegate.unregisterAction(action);
    }

    public static String prettyPrint(Document doc, Node node) {
        DOMImplementationLS ls = (DOMImplementationLS) doc.getImplementation().getFeature("LS", "3.0");
        LSSerializer serializer = ls.createLSSerializer();
        serializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
        LSOutput lsout = ls.createLSOutput();
        lsout.setEncoding("UTF-8");
        StringWriter stringWriter = new StringWriter();
        lsout.setCharacterStream(stringWriter);
        serializer.write(node, lsout);
        return stringWriter.toString();
    }
}
