package com.eventswarm.social.channels;

import com.eventswarm.AddEventAction;
import com.eventswarm.AddEventTrigger;
import com.eventswarm.channels.HttpContentHandler;
import com.eventswarm.events.Header;
import com.eventswarm.events.Source;
import com.eventswarm.events.Sources;
import com.eventswarm.events.jdo.JdoHeader;
import com.eventswarm.events.jdo.OrgJsonEvent;
import com.eventswarm.util.EventTriggerDelegate;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * Class to connect to SuperFeedr and generate events from each item in received notifications from the hub
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class SuperFeedrJsonChannel implements HttpContentHandler, AddEventTrigger {
    private EventTriggerDelegate<AddEventTrigger,AddEventAction> delegate;
    private SuperFeedrSubscriber subscriber;
    private Map<String,URL> subscriptions;
    private long count;
    private long errors;

    public static final String DEFAULT_SOURCE = "superfeedr.com";
    public static final Logger logger = Logger.getLogger(SuperFeedrJsonChannel.class);

    /**
     * Create a new SuperFeedr channel attached to the specified PubSubHubbubSubscriber instance, which we will
     * assume is connected to SuperFeedr.
     *
     * @param subscriber
     */
    public SuperFeedrJsonChannel(SuperFeedrSubscriber subscriber) {
        this.subscriber = subscriber;
        this.delegate = new EventTriggerDelegate<AddEventTrigger,AddEventAction>(this);
        this.subscriptions = new HashMap<String,URL>();
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

    public long getErrors() {
        return errors;
    }

    public long getCount() {
        return count;
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
     * @param subs_id
     * @param body InputStream for reading the HTTP request body
     * @param headers Map of headers
     */
    public void handle(String subs_id, InputStream body, Map<String, List<String>> headers) {
        if (!subscriptions.containsKey(subs_id)) {
            logger.warn("Received content for unrecognised subscription: " + subs_id);
        }
        try {
            JSONObject object = new JSONObject(new JSONTokener(body));
            //logger.debug("JSON received was: " + object.toString(2));
            JSONArray items = object.getJSONArray("items");
            Source source =  getSource(object);
            for (int i=0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String id = getId(item);
                if (id == null) {
                    errors++;
                    logger.error("Could not extract id from JSON item");
                } else {
                    Header header = new JdoHeader(getTimestamp(item), source, getId(item));
                    delegate.fire(new OrgJsonEvent(header, item));
                    count++;
                }
            }
        } catch (JSONException exc) {
            errors++;
        }
    }

    /**
     * Extract the feed host from the notification, falling back on superfeedr if we can't parse the feed URI
     *
     * This default implementation uses the domain name from the status/feed url in the JSON notification, and
     * falls back on the @DEFAULT_SOURCE value if this is not available. Subclasses should override if this strategy
     * is unsuitable.
     *
     * @param notification
     * @return
     */
    protected Source getSource(JSONObject notification) {
        try {
            URI uri = new URI(notification.getJSONObject("status").getString("feed"));
            return Sources.cache.getSourceByName(uri.getHost());
        } catch (URISyntaxException exc) {
            logger.info ("Cannot extract feed host as source, using superfeedr.com");
            return Sources.cache.getSourceByName(DEFAULT_SOURCE);
        } catch (JSONException exc) {
            logger.error("Error extracting feed URL", exc);
            return Sources.cache.getSourceByName(DEFAULT_SOURCE);
        }
    }

    /**
     * Extract an id for the item, ensuring that updated items don't get the same id
     *
     * @param item JSON object from the items list in the notification
     * @return item id or null if this is an update (EventSwarm will assign a UUID if id is null)
     */
    protected String getId(JSONObject item) {
        try {
            if (item.has("updated") && item.getLong("published") < item.getLong("updated")) {
                // this is an item updated, let EventSwarm assign an id by returning null
                logger.info("Item has been updated, using UUID instead of item id");
                return null;
            } else {
                return item.getString("id");
            }
        } catch (JSONException exc) {
            logger.error("Error extracting id from item", exc);
            throw exc;
        }
    }

    /**
     * Method to extract a suitable timestamp from an item
     *
     * Default behaviour is to extract the 'published' element of the entry and create a millisecond timestamp
     * by multiplying by 1000. Subclasses should override if this is not appropriate.
     *
     * @param item
     * @return timestamp for this item
     */
    protected Date getTimestamp(JSONObject item) throws JSONException {
        try {
            return new Date(item.getLong("published")*1000);
        } catch (JSONException exc) {
            logger.error("Error extracting timestamp from item", exc);
            throw exc;
        }
    }

    public void registerAction(AddEventAction action) {
        delegate.registerAction(action);
    }

    public void unregisterAction(AddEventAction action) {
        delegate.unregisterAction(action);
    }
}
