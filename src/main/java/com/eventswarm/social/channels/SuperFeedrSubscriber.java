package com.eventswarm.social.channels;

import com.eventswarm.channels.HttpContentHandler;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Extends PubSubHubbubSubscriber implementation to receive feed notifications from SuperFeedr
 *
 * This class sets some default properties for superfeedr, including selection of format. The class
 * provides a singleton because we don't really want multiple instances in an app.
 *
 * A simplified subscribe method is provided that uses default subscribe/unsubscribe options.
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 */
public class SuperFeedrSubscriber extends PubSubHubbubSubscriber {
    private Map<String,String> options;
    private String callbackUrl;

    /**
     * Properties required to create an instance.
     */
    public static final String PROPS_USERNAME = "username",
                               PROPS_PASSWORD = "password",
                               PROPS_CALLBACK = "callback_url",
                               PROPS_PORT = "port",
                               PROPS_FORMAT = "format";

    public static final String HUB_URL_STRING = "https://push.superfeedr.com",
                               DEFAULT_FORMAT = "json";

    private static final Logger logger = Logger.getLogger(SuperFeedrSubscriber.class);

    private static SuperFeedrSubscriber instance;

    /**
     * Create and return a hub instance using the properties defined in a superfeedr.properties file in the classpath,
     * stopping and removing the current instance (i.e. calling reset()), if it exists.
     *
     * @return hub instance
     * @throws IOException if no properties file exists in the classpath
     * @throws PubSubException if resetting an existing instances causes errors
     */
    public static SuperFeedrSubscriber createInstance() throws PubSubException, IOException {
        if (instance != null) {
            reset();
        }
        Properties props = new Properties();
        props.load(SuperFeedrSubscriber.class.getClassLoader().getResourceAsStream("superfeedr.properties"));
        instance = new SuperFeedrSubscriber(getHubUrl(), props.getProperty(PROPS_CALLBACK),
                   Integer.parseInt(props.getProperty(PROPS_PORT)),
                   props.getProperty(PROPS_USERNAME), props.getProperty(PROPS_PASSWORD));
        setFormat(instance, props.getProperty(PROPS_FORMAT));
        return instance;
    }

    /**
     * Create and return a hub instance using the configuration defined in the supplied conf Map,
     * stopping and removing the current instance (i.e. calling reset()), if it exists.
     *
     * The keys in the conf Map are the same as those used in the properties file. Be aware that an instance
     * will <strong>not</strong> be created if it already exists. If you want to make configuration changes,
     * use reset() first.
     *
     * This method is provided for those who want to get configuration properties from elsewhere.
     *
     * @param conf
     * @return hub instance
     * @throws PubSubException if resetting an existing instances causes errors
     */
    public static SuperFeedrSubscriber createInstance(Map<String,String> conf) throws PubSubException {
        if (instance != null) {
            reset();
        }
        instance = new SuperFeedrSubscriber(getHubUrl(), conf.get(PROPS_CALLBACK),
                   Integer.parseInt(conf.get(PROPS_PORT)), conf.get(PROPS_USERNAME), conf.get(PROPS_PASSWORD));
        setFormat(instance, conf.get(PROPS_FORMAT));
        return instance;
    }

    /**
     * Set the format, defaulting if the supplied format option is null
     *
     * @param instance
     * @param format
     */
    private static void setFormat(SuperFeedrSubscriber instance, String format) {
        if (format != null) {
            instance.getOptions().put(PROPS_FORMAT, format);
        } else {
            instance.getOptions().put(PROPS_FORMAT, DEFAULT_FORMAT);
        }
    }

    /**
     * Get the current instance or null if no instance has been created
     */
    public static SuperFeedrSubscriber getInstance() {
        return instance;
    }

    private static URL getHubUrl() {
        try {
            return new URL(HUB_URL_STRING);
        } catch (MalformedURLException exc) {
            logger.fatal("WTF!!? Cannot create hub URL");
            return null;
        }
    }

    /**
     * Stop the instance, removing any existing subscriptions, and set it to null so that the next
     * call to getInstance creates a new instance.
     *
     * Be aware that the stop() call will delay for a period to allow confirmations to complete.
     *
     * @throws PubSubException if stopping the instance causes any exceptions
     */
    public static void reset() throws PubSubException {
        instance.stop(true);
        instance = null;
    }

    /**
     * Private constructor that that just calls the parent, then sets the standard options for subscribe/unsubscribe
     * requests
     *
     * Standard options are currently "hub.verify" => "async". Superfeedr "format" is set by class instance creation
     * method based on configuration properties.
     *
     * @param hubUrl
     * @param callbackUrl
     * @param port
     * @param user
     * @param password
     */
    private SuperFeedrSubscriber(URL hubUrl, String callbackUrl, int port, String user, String password) {
        super(hubUrl, callbackUrl, port, user, password);
        this.callbackUrl = callbackUrl;
        options = new HashMap<String, String>();
        options.put("hub.verify", "async");
    }


    /**
     * Send a subscribe request with the retrieve option to the superfeedr hub and call the supplied handler
     * with the result of the retrieve
     *
     * This method is intended for backfill, that is, after a restart, fill up the feed with items that we might
     * have missed. We have to rewrite the subscribe request logic from the PubSubHubbubSubscriber class because this is
     * non-standard. Also note that SuperFeedr does not support async verification in this case. Makes for
     * bloody ugly code: really should be handling the protocol in PubSubHubbubSubscriber.
     *
     * @param handler handler for the returned content
     * @param topic topic to retrieve from
     * @param after (optional) retrieve only items published after the identified item
     * @return ID associated with subscription
     * @throws PubSubException
     */
    public String subscribeAndRetrieve(HttpContentHandler handler, URL topic, String after) throws PubSubException {
        try {
            logger.info("Sending " + HTTP_POST + " request to " + getHubUrl().toString() + " to subscribe and retrieve from " + topic.toString());
            String id = makeId(topic, null);
            HttpURLConnection con = getConnection();
            OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
            addParam(out, CALLBACK + "=" + encode(callBack(id).toExternalForm()));
            addParam(out, AMP + TOPIC + "=" + encode(topic.toString()));
            addParam(out, AMP + MODE + "=" + SubRequest.subscribe.toString());
            if (options.get(PROPS_FORMAT) != null) {
                addParam(out, AMP + "format=" + options.get(PROPS_FORMAT));
            }
            addParam(out, AMP + "retrieve=true");
            addParam(out, AMP + "count=50"); // retrieve as many as we can, no point in calling twice
            if (after != null) {
                addParam(out, AMP + "after=" + encode(after));
            }
            super.subs.put(id, topic);
            super.handlers.put(id, handler);
            out.close();
            int code = con.getResponseCode();
            if (code > 299) {
                // deal with a failure
                InputStream in = con.getErrorStream();
                if (in == null) in = con.getInputStream(); // might have returned the wrong code
                String message = "Error in HTTP response, content was: " + readStream(in);
                super.subs.remove(id);
                super.handlers.remove(id);
                logger.error(message);
                throw new PubSubException(message);
            } else {
                logger.debug("Received HTTP response with code " + Integer.toString(code));
                handler.handle(id, con.getInputStream(), con.getHeaderFields());
                return id;
            }
        } catch (Exception exc) {
            String message = "Error sending retrieve request";
            logger.error(message, exc);
            throw new PubSubException(message, exc);
        }
    }

    /**
     * Get the subscription options associated with this instance.
     *
     * These options are passed as URL-encoded parameters with any subscribe or unsubscribe request. It is assumed
     * that all subscriptions will use the same options.  Note that the returned Map reference allows you to change
     * the options, so no setOptions() method is provided.
     *
     * If you need to set options per-request, use the subscribe method provided by the parent class.
     *
     * @return Current options Map
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Subscribe a handler to a topic using currently defined options.
     *
     * @see PubSubHubbubSubscriber#subscribe method for more detail
     *
     * @param handler
     * @param topic
     * @return
     * @throws PubSubException
     */
    public String subscribe(HttpContentHandler handler, URL topic) throws PubSubException {
        logger.debug("Subscribing with options: " + options.toString());
        return super.subscribe(handler, topic, options);
    }
}
