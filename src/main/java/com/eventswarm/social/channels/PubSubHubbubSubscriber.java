package com.eventswarm.social.channels;

import com.sun.net.httpserver.*;
import org.apache.log4j.Logger;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * Abstract class implementing the PubSubHubbubSubscriber subscriber protocol without concern for the payload (i.e. doesn't know
 * how to create events from the payload).
 *
 * The classes uses the JDK httpserver to receive requests from a hub and an HTTPUrlConnection to send
 * subscribe/unsubscribe requests to the hub. Subscriptions are distinguished by an id, which should be
 * a URL-safe string. A distinct channel is created for each id. HTTPS is not directly supported: if you need HTTPS,
 * use a reverse proxy like nginx or apache httpd.
 *
 * This class <strong>does not</strong> implement discovery, nor does it check that the topic URLs are advertised by
 * the publisher. As such, it is not strictly compliant with the
 * <a href="https://pubsubhubbub.googlecode.com/svn/trunk/pubsubhubbub-core-0.4.html">v0.4 spec</a>.
 * Callers must verify topic URLs themselves, either manually or otherwise.
 *
 * Concrete implementations must provide methods to create events for each notification. This implementation does not
 * currently implement authentication or verification of content encrypted with a secret. Concrete implementations
 * should also implement suitably narrowed subscribe and unsubscribe methods for different hubs.
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class PubSubHubbubSubscriber implements HttpHandler {
    private URL hubUrl;
    private String pushUrl;
    protected Map<String,PubSubContentHandler> handlers; // protected so we can access in test
    protected Map<String,URL> subs, unsubs; // protected so we can access in test
    protected Map<String,Boolean> confirmed; // record subscribe confirmations
    private int port;
    private String user;
    private String password;
    private HttpServer server;
    private MessageDigest digest;

    public static enum SubRequest {subscribe, unsubscribe};
    public static final String ENCODING = "UTF8",
                               HTTP_POST = "POST",
                               HTTP_GET = "GET",
                               HTTP_AUTHORIZATION = "Authorization",
                               AMP = "&",
                               TOPIC = "hub.topic",
                               MODE = "hub.mode",
                               CALLBACK = "hub.callback",
                               CHALLENGE = "hub.challenge",
                               DIGEST_ALG = "SHA1";

    // Use a constant SALT for hard-to-guess callback IDs so that we can survive restarts
    public static final byte[] SALT = "EventSwarm".getBytes();

    private static Logger logger = Logger.getLogger(PubSubHubbubSubscriber.class);

    /**
     * Create a channel receiving notifications from the specified hub on the specified base url
     *
     * Currently assumes HTTP basic auth (implication: please use an HTTPS URL for the hub)
     *
     * @param hubUrl Feed URL of PubSubHubbubSubscriber hub server
     * @param pushUrl Base URL for receiving notifications, no parameters, no trailing '/', we assume proper encoding
     * @param port Port number for listening to receive requests
     * @param user User name for access to hub (if null, no credentials are sent)
     * @param password Password for access to the hub
     */
    public PubSubHubbubSubscriber(URL hubUrl, String pushUrl, int port, String user, String password) {
        logger.debug("Creating new instance");
        this.hubUrl = hubUrl;
        // strip trailing '/' from URL if present
        this.pushUrl = pushUrl;
        this.handlers = new HashMap<String,PubSubContentHandler>();
        this.subs = new HashMap<String, URL>();
        this.unsubs = new HashMap<String, URL>();
        this.confirmed = new HashMap<String,Boolean>();
        this.port = port;
        this.user = user;
        this.password = password;
        try {
            digest = MessageDigest.getInstance(DIGEST_ALG);
        } catch (NoSuchAlgorithmException exc) {
            // should never get here
            logger.fatal("WTF!!? Cannot use " + DIGEST_ALG + " digest algorithm", exc);
        }
    }

    /**
     * Subscribe to a topic and deliver events from that topic to a channel
     *
     * The returned id is used as a key for the subscription. Callers must store it safely to ensure that the
     * topic can be unsubscribed. Distinguished ids are generated using a digest of the topic URL, other parameters
     * and a salt to make it reasonably random and hard to guess.
     *
     * Only a single content handler can be associated with a topic + other params. A PubSubException will be thrown
     * if any attempt is made to subscribe a second handler on the same topic. Duplicate requests will be ignored
     * (i.e. if you're already registered as a handler for this topic + params, nothing will change.
     *
     * @param handler A PubSubContentHandler implementation to handle notifications for the topic
     * @param topic Topic URL to be monitored by the hub, caller is responsible for ensuring hub can monitor this URL
     * @param other Other parameters to send with the subscription request (in addition to topic, callback and mode)
     * @return id associated with topic or null if topic is already subscribed
     */
    public String subscribe(PubSubContentHandler handler, URL topic, Map<String,String> other) throws PubSubException {
        String id = makeId(topic, other);
        if (!handlers.containsKey(id)) {
            // new subscription, add handler and subscribe
            handlers.put(id, handler);
            subs.put(id, topic); // update the subscription to add the new topic URL
            if (!send(SubRequest.subscribe, topic, callBack(id), other)) {
                // send failed, so drop the subscription and handler
                handlers.remove(id);
                subs.remove(id);
            }
        } else if (handlers.get(id) == handler) {
            return id;
        } else {
            throw new PubSubException("Topic already subscribed");
        }
        return id;
    }

    /**
     * Create a deterministic, hard-to-guess string id from the topic and other params using a one-way hash
     *
     * This id is used both as a hash key for subscriptions and as the trailing string in a callback URL
     *
     * @param topic
     */
    protected String makeId(URL topic, Map<String,String> other) {
        digest.update(SALT);
        digest.update(topic.toString().getBytes());
        if (other != null) {
            logger.debug("Adding parameter keys and values to digest bytes");
            for (String key : new TreeSet<String>(other.keySet())) {
                digest.update(key.getBytes());
                digest.update(other.get(key).getBytes());
            }
        }
        return DatatypeConverter.printBase64Binary(digest.digest());
    }

    /**
     * Unsubscribe from the topic associated with the specified id and remove the associated handler
     *
     * A PubSubException will be thrown if the handler is not subscribed to the topic
     *
     * @param id subscription id to unsubscribe
     * @param handler Handler to unsubscribe
     * @throws PubSubHubbubSubscriber.PubSubException
     */
    public void unsubscribe(String id, PubSubContentHandler handler) throws PubSubException {
        if (handlers.get(id) == handler) {
            // this is the right handler for the subscription, so unsubscribe
            unsubscribe(id);
        } else {
            // otherwise throw an exception
            throw new PubSubException("Handler not subscribed");
        }
    }

    /**
     * Send an unsubscribe request for the topic and callback associated with the supplied id
     *
     * @param id
     */
    private void unsubscribe(String id) throws PubSubException {
        unsubs.put(id, subs.get(id));
        subs.remove(id);
        confirmed.remove(id);
        send(SubRequest.unsubscribe, unsubs.get(id), callBack(id), null);
        handlers.remove(id);
    }

    /**
     * Set the executor associated with the HttpServer instance
     *
     * @param executor
     */
    public void setExecutor(Executor executor) {
        server.setExecutor(executor);
    }

    /**
     * Get the executor associated with the HttpServer instance
     */
    public Executor getExecutor() {
        return server.getExecutor();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Start the HTTP server for receiving PubSubHubbubSubscriber requests.
     *
     * @throws PubSubException
     */
    public void start() throws PubSubException {
        if (this.server == null) {
            try {
                logger.debug("Starting server on port " + Integer.toString(port));
                this.server = HttpServer.create(new InetSocketAddress((InetAddress) null, port), 0);
                this.server.createContext("/", this); // set up this instance as the handler
                this.server.start();
            } catch (IOException exc) {
                String message = "Could not start server on port " + Integer.toString(port);
                logger.error(message, exc);
                throw new PubSubException(message, exc);
            }
        }
    }

    /**
     * Stop the HTTP server and optionally unsubscribe all subscriptions and remove handlers
     */
    public void stop(boolean unsubscribe) throws PubSubException {
        if (this.server != null) {
            if (unsubscribe) {
                for (String id : subs.keySet()) {
                    unsubscribe(id);
                }
                // allow some time for the unsubscribe requests to be confirmed
                server.stop(10);
            } else {
                server.stop(0);
            }
        }
    }

    /**
     * Implement the handle method required by the HttpServer
     *
     * @param exchange
     */
    @Override
    public void handle(HttpExchange exchange) {
        URI uri = exchange.getRequestURI();
        String subs_id = getId(uri);
        logger.debug("Received content for subscription id: " + subs_id);
        try {

            if (exchange.getRequestMethod().equals(HTTP_GET)) {
                // subs or unsubs verification, handle it
                handleSubs(exchange);
            } else if (exchange.getRequestMethod().equals(HTTP_POST)) {
                // this is a notification, call the content handlers
                logger.debug("Received content at " + uri.toString() + " from " + exchange.getRemoteAddress().getHostName());
                // TODO implement some verification of link headers to verify that this is a valid request
                PubSubContentHandler handler = handlers.get(subs_id);
                if (handler == null) {
                    logger.warn("Received content for non-existent subscription, discarding");
                    respond(exchange, "", 201);
                } else {
                    handler.handle(subs_id, exchange.getRequestBody(), exchange.getRequestHeaders());
                    respond(exchange, "", 201);
                }
            } else {
                logger.error("Unhandled request method: " + exchange.getRequestMethod());
                respond(exchange, "", 500);
            }
        } catch (Exception exc) {
            String message = "Error receiving request on URL " + uri.toString();
            logger.error(message, exc);
        } finally {
            exchange.close();
        }
    }

    /**
     * Deal with subscribe and unsubscribe verifications
     *
     * @param exchange
     * @throws IOException
     */
    private void handleSubs(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String id = getId(uri);
        logger.debug("Extracted subscription id: " + id);
        Map<String,String> params = getParams(exchange.getRequestURI());
        String mode = params.get(MODE);
        // We keep subs/unsub -> topic mappings in distinct maps
        Map<String,URL> subsCopy = SubRequest.unsubscribe.toString().equals(mode) ? unsubs : this.subs;
        if (!subsCopy.containsKey(id)) {
            logger.error ("Unrecognised subscription in URL " + uri.toString());
            respond(exchange, "", 404);
        } else if (!subsCopy.get(id).toString().equals(params.get(TOPIC))) {
            logger.error("No matching topic " + uri.toString());
            respond(exchange, "", 404);
        } else {
            logger.info (mode + " id " + id + ". Request was " + uri.toString() + ". Responding with " + params.get(CHALLENGE));
            respond(exchange, params.get(CHALLENGE), 200);
            if (subsCopy == unsubs) {
                unsubs.remove(id);
            } else if (subsCopy == subs) {
                confirmed.put(id, true);
            }
        }
    }

    /**
     * Return the id we have associated with a subscription, which is the leaf of the path in the request URI.
     *
     * @param uri Incoming request URI
     * @return
     */
    public String getId(URI uri) {
        String path = uri.getRawPath();
        logger.debug("Have request path: " + path);
        return decode(path.substring(path.lastIndexOf('/') + 1));
    }

    /**
     * Respond to a HttpServer request with the specified content and HTTP response code
     *
     * @param exchange
     * @param content
     * @param code
     * @throws IOException
     */
    private void respond(HttpExchange exchange, String content, int code) throws IOException {
        if (content == null) content = "";
        exchange.sendResponseHeaders(code, content.length());
        exchange.getResponseBody().write(content.getBytes());
        exchange.getResponseBody().close();
    }

    /**
     * Extract the parameters encoded in a URI into a Map instance
     *
     * @param uri
     * @return
     */
    private Map<String,String> getParams(URI uri) {
        String[] params = uri.getQuery().split("&");
        Map<String,String> result = new HashMap<String,String>();
        if (params != null && params.length > 0) {
            for (int i=0; i < params.length; i++) {
                int idx = params[i].indexOf('=');
                if (idx < 1) {
                    result.put(params[i], null);
                } else {
                    result.put(params[i].substring(0,idx), params[i].substring(idx+1));
                }
            }
        }
        return result;
    }

    /**
     * Generate a callback URL from the generated id string by adding it to our base URL
     *
     * This method assumes the id is not already URL-encoded.
     *
     * @param id
     * @return
     * @throws PubSubException
     */
    protected URL callBack(String id) throws PubSubException {
        try {
            return new URL(pushUrl + "/" + encode(id));
        } catch (MalformedURLException exc) {
            logger.error(exc);
            throw new PubSubException("Could not create URL for " + pushUrl + "/" + id, exc);
        }
    }

    /**
     * Encode a string for inclusion in a URL or parameter list
     *
     * @param text
     * @return
     */
    protected static String encode(String text) {
        try {
            return URLEncoder.encode(text, ENCODING);
        } catch (UnsupportedEncodingException exc) {
            // should never get here
            logger.fatal("WTF!!? " + ENCODING + " is not a valid encoding", exc);
            return null;
        }
    }

    /**
     * Decode a string included in a URL or parameter list
     *
     * @param text
     * @return
     */
    protected static String decode(String text) {
        try {
            return URLDecoder.decode(text, "UTF8");
        } catch (UnsupportedEncodingException exc) {
            // should never get here
            System.out.println("WTF!!? UTF8 is not a valid encoding. Exception: " + exc.getMessage());
            return null;
        }
    }

    /**
     * Send a sub/unsub request to the hub
     *
     * @return
     * @throws PubSubHubbubSubscriber.PubSubException
     */
    protected boolean send(SubRequest request, URL topic, URL callback, Map<String,String> other) throws PubSubException {
        try {
            logger.info("Sending POST request to " + hubUrl.toString() + " with mode " + request.toString()
                    + " and callback " + callback.toString());
            HttpURLConnection con = getConnection();
            OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
            addParam(out, CALLBACK + "=" + encode(callback.toString()));
            addParam(out, AMP + TOPIC + "=" + encode(topic.toString()));
            addParam(out, AMP + MODE + "=" + request.toString());
            addOtherParams(out,other);
            out.close();
            int code = con.getResponseCode();
            logger.info("Received HTTP response with code " + Integer.toString(code));
            if (code > 299) {
                // subscription and related requests should always return a 202, but this is not consistent
                // so accept any 2XX response
                InputStream in = con.getErrorStream();
                if (in == null) in = con.getInputStream(); // might have returned the wrong code
                logger.error("Error in response, content was: " + readStream(in));
                return false;
            } else {
                con.getInputStream().close();
                return true;
            }
        } catch (Exception exc) {
            String message = "Error sending " + request.toString() + " request";
            logger.error(message, exc);
            throw new PubSubException(message, exc);
        }
    }


    protected void addOtherParams(OutputStreamWriter out, Map<String,String> params) throws IOException {
        if (params != null) {
            for(String key : params.keySet()) {
                addParam(out, AMP + encode(key) + "=" + encode(params.get(key)));
            }
        }
    }

    protected void addParam(OutputStreamWriter out, String param) throws IOException {
        logger.debug("Setting HTTP param: " + param);
        out.write(param);
    }

    protected static String readStream(InputStream in) throws IOException {
        Scanner scanner = new Scanner(in);
        StringBuilder result = new StringBuilder();
        while (scanner.hasNextLine()) {
            result.append(scanner.nextLine());
        }
        return result.toString();
    }

    /**
     * Grab a POST connection to the hub URL with appropriate headers, including auth if defined
     *
     * @return
     * @throws IOException
     */
    protected HttpURLConnection getConnection() throws IOException {
        HttpURLConnection con = (HttpURLConnection) hubUrl.openConnection();
        con.setRequestMethod(HTTP_POST);
        setHttpAuthorization(con);
        con.setDoOutput(true);
        con.connect();
        return con;
    }

    protected void setHttpAuthorization(HttpURLConnection con) {
        if (user != null) {
            con.setRequestProperty(HTTP_AUTHORIZATION, "Basic " +
                    DatatypeConverter.printBase64Binary((user + ":" + password).getBytes()));
        }
    }

    /**
     * Simple exception class to wrap exceptions
     */
    public static class PubSubException extends Exception {
        public PubSubException(String message) {
            super(message);
        }

        public PubSubException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
