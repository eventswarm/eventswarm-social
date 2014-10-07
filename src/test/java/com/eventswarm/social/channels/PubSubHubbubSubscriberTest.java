package com.eventswarm.social.channels;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 3/10/2014
 * Time: 11:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class PubSubHubbubSubscriberTest implements HttpHandler, PubSubContentHandler {
    URL hubURL, subURL;
    HttpServer hub;
    HttpExchange hubRequest;
    String hubRequestContent;
    int hubResponseCode;
    String hubResponseContent;
    String subResponse;
    int subResponseCode;
    String hubContent;
    Map<String,List<String>> hubHeaders;
    Exception notifyFailed;
    byte[] buffer;
    boolean otherCalled;
    String content;

    @BeforeClass
    public static void setup() throws Exception {
        BasicConfigurator.configure();
    }

    @Before
    public void setUp() throws Exception {
        hubURL = new URL("http://localhost:54321");
        hub = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), hubURL.getPort()), 0);
        hub.createContext("/", this);
        hub.start();
        subURL = new URL("http://localhost:5432");
        notifyFailed = null;
    }

    @After
    public void tearDown() throws Exception {
        hub.stop(1);
    }

    @Test
    public void testConstruct() throws Exception {
        PubSubHubbubSubscriber instance = new PubSubHubbubSubscriber(hubURL, subURL.toExternalForm(), subURL.getPort(), null, null);
        assertNotNull(instance);
        assertNotNull(instance.handlers);
        assertNotNull(instance.subs);
        assertNotNull(instance.unsubs);
    }

    @Test
    public void testSubscribe() throws Exception {
        PubSubHubbubSubscriber instance = new PubSubHubbubSubscriber(hubURL, subURL.toString(), subURL.getPort(), null, null);
        instance.start();
        hubResponseCode = 202;
        String id = instance.subscribe(this, new URL("http://myrssfeed.com"), null);
        System.out.println("PubSubHubub subscription id: " + id);
        assertNotNull(hubRequest);
        assertTrue(instance.subs.containsKey(id));
        assertEquals(this, instance.handlers.get(id));
        System.out.println("Hub Received content: " + hubRequestContent);
        Map<String,String> params = getParams(hubRequestContent);
        assertTrue(params.containsKey("hub.topic"));
        assertEquals("http://myrssfeed.com", decode(params.get("hub.topic")));
        assertTrue("subscribe", params.containsKey("hub.mode"));
        assertEquals(subURL.toString() + "/" + encode(id), decode(params.get("hub.callback")));
        instance.stop(false);
    }

    @Test
    public void testSubscribeSecondHandler() throws Exception {
        PubSubHubbubSubscriber instance = new PubSubHubbubSubscriber(hubURL, subURL.toString(), subURL.getPort(), null, null);
        instance.start();
        hubResponseCode = 202;
        String id = instance.subscribe(this, new URL("http://myrssfeed.com"), null);
        System.out.println("PubSubHubub subscription id: " + id);
        PubSubContentHandler other = new PubSubContentHandler() {
            @Override
            public void handle(InputStream body, Map<String, List<String>> headers) {
                // do nothing
            }
        };
        try {
            String id2 = instance.subscribe(other, new URL("http://myrssfeed.com"), null);
            fail("Second subscription should be refused");
        } catch (PubSubHubbubSubscriber.PubSubException exc) {
            assertTrue(instance.subs.containsKey(id));
            assertEquals(this, instance.handlers.get(id));
        }
        instance.stop(false);
    }

    @Test
    public void testUnsubscribe() throws Exception {
        PubSubHubbubSubscriber instance = new PubSubHubbubSubscriber(hubURL, subURL.toString(), subURL.getPort(), null, null);
        instance.start();
        hubResponseCode = 202;
        String id = instance.subscribe(this, new URL("http://myrssfeed.com"), null);
        instance.unsubscribe(id, this);
        assertNotNull(hubRequest);
        assertFalse(instance.subs.containsKey(id));
        assertFalse(instance.handlers.containsKey(id));
        assertTrue(instance.unsubs.containsKey(id));
        System.out.println("Hub Received content: " + hubRequestContent);
        Map<String,String> params = getParams(hubRequestContent);
        assertEquals("http://myrssfeed.com", decode(params.get("hub.topic")));
        assertTrue("unsubscribe", params.containsKey("hub.mode"));
        assertEquals(subURL.toString() + "/" + encode(id), decode(params.get("hub.callback")));
        instance.stop(false);
    }

    @Test
    public void testStopAndUnsubscribe() throws Exception {
        PubSubHubbubSubscriber instance = new PubSubHubbubSubscriber(hubURL, subURL.toString(), subURL.getPort(), null, null);
        instance.start();
        hubResponseCode = 202;
        String id = instance.subscribe(this, new URL("http://myrssfeed.com"), null);
        instance.stop(true);
        assertEquals(0, instance.subs.size());
        assertEquals(1, instance.unsubs.size());
        assertEquals(0, instance.handlers.size());
        Map<String,String> params = getParams(hubRequestContent);
        assertEquals("http://myrssfeed.com", decode(params.get("hub.topic")));
        assertTrue("unsubscribe", params.containsKey("hub.mode"));
        assertEquals(subURL.toString() + "/" + encode(id), decode(params.get("hub.callback")));
        instance.stop(false);
    }

    @Test
    public void testHandleSubConfirm() throws Exception {
        PubSubHubbubSubscriber instance = new PubSubHubbubSubscriber(hubURL, subURL.toString(), subURL.getPort(), null, null);
        instance.start();
        hubResponseCode = 202;
        String id = instance.subscribe(this, new URL("http://myrssfeed.com"), null);
        System.out.println("Subscription id is: " + id);
        subGet(id, "hub.mode=subscribe&hub.topic=" + encode("http://myrssfeed.com") + "&hub.challenge=blahblah&hub.lease_seconds=3600");
        assertEquals(200, subResponseCode);
        assertEquals("blahblah", subResponse);
        instance.stop(false);
    }

    @Test
    public void testHandleSubBadId() throws Exception {
        PubSubHubbubSubscriber instance = new PubSubHubbubSubscriber(hubURL, subURL.toString(), subURL.getPort(), null, null);
        instance.start();
        hubResponseCode = 202;
        String id = instance.subscribe(this, new URL("http://myrssfeed.com"), null);
        System.out.println("Subscription id is: " + id);
        subGet("blahblah", "hub.mode=subscribe&hub.topic=" + encode("http://myrssfeed.com") + "&hub.challenge=blahblah&hub.lease_seconds=3600");
        assertEquals(404, subResponseCode);
        instance.stop(false);
    }

    @Test
    public void testHandleSubBadTopic() throws Exception {
        PubSubHubbubSubscriber instance = new PubSubHubbubSubscriber(hubURL, subURL.toString(), subURL.getPort(), null, null);
        instance.start();
        hubResponseCode = 202;
        String id = instance.subscribe(this, new URL("http://myrssfeed.com"), null);
        System.out.println("Subscription id is: " + id);
        subGet("blahblah", "hub.mode=subscribe&hub.topic=" + encode("http://myrssfeed.co.uk") + "&hub.challenge=blahblah&hub.lease_seconds=3600");
        assertEquals(404, subResponseCode);
        instance.stop(false);
    }

    @Test
    public void testHandleUnSubConfirm() throws Exception {
        PubSubHubbubSubscriber instance = new PubSubHubbubSubscriber(hubURL, subURL.toString(), subURL.getPort(), null, null);
        instance.start();
        hubResponseCode = 202;
        String id = instance.subscribe(this, new URL("http://myrssfeed.com"), null);
        instance.unsubscribe(id, this);
        System.out.println("Subscription id is: " + id);
        subGet(id, "hub.mode=unsubscribe&hub.topic=" + encode("http://myrssfeed.com") + "&hub.challenge=blahblah&hub.lease_seconds=3600");
        assertEquals(200, subResponseCode);
        assertEquals("blahblah", subResponse);
        assertFalse(instance.unsubs.containsKey(id));
        instance.stop(false);
    }

    @Test
    public void testHandleNotify() throws Exception {
        PubSubHubbubSubscriber instance = new PubSubHubbubSubscriber(hubURL, subURL.toString(), subURL.getPort(), null, null);
        instance.start();
        hubResponseCode = 202;
        String id = instance.subscribe(this, new URL("http://myrssfeed.com"), null);
        content = "This is my content";
        subPost(id, content);
        assertEquals(content, hubContent);
        instance.stop(false);
    }

    @Test
    public void testHandleRemoteSubConfirm() throws Exception {
        PubSubHubbubSubscriber instance = new PubSubHubbubSubscriber(hubURL, subURL.toString(), subURL.getPort(), null, null);
        instance.start();
        hubResponseCode = 202;
        String id = instance.subscribe(this, new URL("http://myrssfeed.com"), null);
        System.out.println("Subscription id is: " + id);
        subURL = new URL("http://pubsub.whyanbeel.net");
        subGet(id, "hub.mode=subscribe&hub.topic=" + encode("http://myrssfeed.com") + "&hub.challenge=blahblah&hub.lease_seconds=3600");
        assertEquals(200, subResponseCode);
        assertEquals("blahblah", subResponse);
        instance.stop(false);
    }

    @Test
    public void testRealSubscribe() throws Exception {
        PubSubHubbubSubscriber instance = null;
        try {
            Properties props = new Properties();
            props.load(this.getClass().getClassLoader().getResourceAsStream("superfeedr.properties"));
            instance = new PubSubHubbubSubscriber(new URL(props.getProperty("hub_url")), props.getProperty("callback_url"),
                    subURL.getPort(), props.getProperty("username"), props.getProperty("password"));
            instance.start();
            Map<String,String> options = new HashMap<String,String>();
            options.put("hub.verify", "async");
            String id = instance.subscribe(this, new URL("http://blog.superfeedr.com"), options);
            System.out.println("Sleeping to wait for the confirmation");
            Thread.sleep(4000); // give superfeedr a chance to respond
            assertTrue("Subscription id should be confirmed", instance.confirmed.containsKey(id));
            assertTrue("Confirmation entry should be true", instance.confirmed.get(id));
        } finally {
            if (instance != null) {instance.stop(true);}
        }
    }

    @Test
    public void testRealNotify() throws Exception {
        PubSubHubbubSubscriber instance = null;
        try {
            Properties props = new Properties();
            props.load(this.getClass().getClassLoader().getResourceAsStream("superfeedr.properties"));
            instance = new PubSubHubbubSubscriber(new URL(props.getProperty("hub_url")), props.getProperty("callback_url"),
                    subURL.getPort(), props.getProperty("username"), props.getProperty("password"));
            instance.start();
            Map<String,String> options = new HashMap<String,String>();
            options.put("hub.verify", "async");
            String id = instance.subscribe(this, new URL("http://push-pub.appspot.com/feed"), options);
            Thread.sleep(10000);
        } finally {
            if (instance != null) {instance.stop(true);}
        }
    }

    @Override
    public void handle(InputStream body, Map<String, List<String>> headers) {
        try {
            hubContent = readStream(body);
            System.out.println("Received content:" + hubContent);
        } catch (Exception exc) {
            notifyFailed = exc;
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        hubRequest = exchange;
        hubRequestContent = readStream(exchange.getRequestBody());
        String content = (hubResponseContent == null) ? "" : hubResponseContent;
        exchange.sendResponseHeaders(hubResponseCode, content.length());
        exchange.getResponseBody().write(content.getBytes());
        exchange.getResponseBody().close();
    }

    private void subGet(String id, String params) throws Exception {
        URL url = new URL(subURL.toString() + "/" + encode(id) + "?" + params);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.connect();
        setSubResponse(con);
    }

    private void subPost(String id, String content) throws Exception {
        URL url = new URL(subURL.toString() + "/" + id);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.connect();
        con.getOutputStream().write(content.getBytes());
        con.getOutputStream().close();
        setSubResponse(con);
    }

    private void setSubResponse(HttpURLConnection con) throws Exception {
        subResponseCode = con.getResponseCode();
        InputStream in = (subResponseCode > 299) ? con.getErrorStream() : con.getInputStream();
        subResponse = readStream(in);
    }

    private String readStream(InputStream in) throws IOException {
        Scanner scanner = new Scanner(in);
        StringBuffer result = new StringBuffer();
        while (scanner.hasNextLine()) {
            result.append(scanner.nextLine());
        }
        return result.toString();
    }

    private Map<String,String> getParams(String content) {
        String[] params = content.split("&");
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
        System.out.println("Parameters are " + result.toString());
        return result;
    }

    private String decode(String text) {
        try {
            return URLDecoder.decode(text, "UTF8");
        } catch (UnsupportedEncodingException exc) {
            // should never get here
            System.out.println("WTF!!? UTF8 is not a valid encoding. Exception: " +  exc.getMessage());
            return null;
        }
    }

    private String encode(String text) {
        try {
            return URLEncoder.encode(text, "UTF8");
        } catch (UnsupportedEncodingException exc) {
            // should never get here
            System.out.println("WTF!!? UTF8 is not a valid encoding. Exception: " + exc.getMessage());
            return null;
        }
    }
}
