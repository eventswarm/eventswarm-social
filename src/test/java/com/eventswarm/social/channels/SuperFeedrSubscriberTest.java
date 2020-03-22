package com.eventswarm.social.channels;

import com.eventswarm.channels.HttpContentHandler;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import static org.hamcrest.CoreMatchers.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class SuperFeedrSubscriberTest implements HttpContentHandler {
    private static enum CONTENT_TYPE {atom, json, html};
    private static Pattern JSON_START = Pattern.compile("^\\s*\\{");
    private static Pattern XML_START = Pattern.compile("^\\s*<\\?xml");

    private String hubContent;
    private Exception notifyFailed;
    private static DocumentBuilder builder;
    private CONTENT_TYPE type;
    private org.w3c.dom.Document xmlContent;
    private JSONObject jsonContent;
    private org.jsoup.nodes.Document htmlContent;


    @BeforeClass
    public static void setup() throws Exception {
        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    @Test
    public void testCreateInstanceProps() throws Exception {
        SuperFeedrSubscriber instance = SuperFeedrSubscriber.createInstance();
        assertNotNull(instance);
        assertEquals(instance, SuperFeedrSubscriber.getInstance());
        assertNotNull(instance.getOptions());
        assertEquals("json", instance.getOptions().get("format"));
        assertEquals("async", instance.getOptions().get("hub.verify"));
        assertEquals("note8", instance.getUser());
    }

    @Test
    public void testCreateInstanceMap() throws Exception {
        Map<String,String> conf = new HashMap<String,String>();
        conf.put(SuperFeedrSubscriber.CALLBACK, "http://localhost:5432");
        conf.put("username", "andyb");
        conf.put("password", "blahblah");
        conf.put("port", "1234");
        conf.put("callback_url", "http://localhost:1234");
        SuperFeedrSubscriber instance = SuperFeedrSubscriber.createInstance(conf);
        assertNotNull(instance);
        assertEquals(instance, SuperFeedrSubscriber.getInstance());
        assertNotNull(instance.getOptions());
        assertEquals("andyb", instance.getUser());
        assertEquals("blahblah", instance.getPassword());
    }

    @Ignore("Needs working callback URL in $PROJECT_HOME/../superfeedr.properties")
    @Test
    public void testSubscribe() throws Exception {
        SuperFeedrSubscriber instance = null;
        type = CONTENT_TYPE.json;
        try {
            instance = SuperFeedrSubscriber.createInstance();
            instance.start();
            String id = instance.subscribe(this, new URL("http://push-pub.appspot.com/feed"));
            assertNotNull("ID for subscription should be returned", id);
            Thread.sleep(4000);
            assertNotNull("Subscription should be confirmed", instance.confirmed.get(id));
            assertTrue("Confirmation flag should be true", instance.confirmed.get(id));
        } finally {
            if (instance != null) {instance.stop(true);}
        }
    }

    @Ignore("Needs working callback URL in $PROJECT_HOME/../superfeedr.properties")
    @Test
    public void testSubscribeAndRetrieveJson() throws Exception {
        SuperFeedrSubscriber instance = null;
        type = CONTENT_TYPE.json;
        try {
            instance = SuperFeedrSubscriber.createInstance();
            instance.start();
            String id = instance.subscribeAndRetrieve(this, new URL("http://push-pub.appspot.com/feed"), null);
            assertNotNull(hubContent);
            // make sure we can parse it as JSON
            assertNotNull(instance.subs.get(id));
            assertNotNull(instance.handlers.get(id));
        } finally {
            if (instance != null) {instance.stop(true);}
        }
    }

    @Ignore("Needs working callback URL in $PROJECT_HOME/../superfeedr.properties")
    @Test
    public void testSubscribeAndRetrieveJsonWait() throws Exception {
        SuperFeedrSubscriber instance = null;
        type = CONTENT_TYPE.json;
        try {
            instance = SuperFeedrSubscriber.createInstance();
            instance.start();
            String id = instance.subscribeAndRetrieve(this, new URL("http://push-pub.appspot.com/feed"), null);
            assertNotNull(hubContent);
            // make sure we can parse it as JSON
            JSONObject prev = jsonContent;
            assertNotNull(instance.subs.get(id));
            assertNotNull(instance.handlers.get(id));
            waitForPublish();
            assertThat(jsonContent, not(equalTo(prev)));
        } finally {
            if (instance != null) {instance.stop(true);}
        }
    }

    @Ignore("Needs working callback URL in $PROJECT_HOME/../superfeedr.properties")
    @Test
    public void testSubscribeAndRetrieveAtom() throws Exception {
        SuperFeedrSubscriber instance = null;
        type = CONTENT_TYPE.atom;
        try {
            instance = SuperFeedrSubscriber.createInstance();
            instance.start();
            instance.getOptions().put(SuperFeedrSubscriber.PROPS_FORMAT, "atom");
            String id = instance.subscribeAndRetrieve(this, new URL("http://push-pub.appspot.com/feed"), null);
            assertNotNull(xmlContent);
            // TODO: make sure we can parse it as XML
            assertNotNull(instance.subs.get(id));
            assertNotNull(instance.handlers.get(id));
        } finally {
            if (instance != null) {instance.stop(true);}
        }
    }

    @Ignore("Needs working callback URL in $PROJECT_HOME/../superfeedr.properties")
    @Test
    public void testSubscribeAndRetrieveAtomWait() throws Exception {
        SuperFeedrSubscriber instance = null;
        type = CONTENT_TYPE.atom;
        try {
            instance = SuperFeedrSubscriber.createInstance();
            instance.start();
            instance.getOptions().put(SuperFeedrSubscriber.PROPS_FORMAT, "atom");
            String id = instance.subscribeAndRetrieve(this, new URL("http://push-pub.appspot.com/feed"), null);
            assertNotNull(xmlContent);
            org.w3c.dom.Document prev = xmlContent;
            // TODO: make sure we can parse it as XML
            assertNotNull(instance.subs.get(id));
            assertNotNull(instance.handlers.get(id));
            waitForPublish();
            assertNotNull(xmlContent);
            assertThat(xmlContent, not(equalTo(prev)));
        } finally {
            if (instance != null) {instance.stop(true);}
        }
    }

    @Ignore("Needs working callback URL in $PROJECT_HOME/../superfeedr.properties")
    @Test
    public void testSubscribeWeb() throws Exception {
        SuperFeedrSubscriber instance = null;
        type = CONTENT_TYPE.html;
        try {
            instance = SuperFeedrSubscriber.createInstance();
            instance.start();
            String id = instance.subscribe(this, new URL("http://push-pub.appspot.com#article.h-entry"));
            assertNotNull("ID for subscription should be returned", id);
            Thread.sleep(4000);
            assertNotNull("Subscription should be confirmed", instance.confirmed.get(id));
            assertTrue("Confirmation flag should be true", instance.confirmed.get(id));
        } finally {
            if (instance != null) {instance.stop(true);}
        }
    }

    @Ignore("Needs working callback URL in $PROJECT_HOME/../superfeedr.properties")
    @Test
    public void testSubscribeWebAndWait() throws Exception {
        SuperFeedrSubscriber instance = null;
        type = CONTENT_TYPE.html;
        try {
            instance = SuperFeedrSubscriber.createInstance();
            instance.start();
            String id = instance.subscribe(this, new URL("http://push-pub.appspot.com#article.h-entry"));
            assertNotNull("ID for subscription should be returned", id);
            Thread.sleep(4000);
            assertNotNull("Subscription should be confirmed", instance.confirmed.get(id));
            assertTrue("Confirmation flag should be true", instance.confirmed.get(id));
            waitForPublish();
            assertNotNull(htmlContent);
        } finally {
            if (instance != null) {instance.stop(true);}
        }
    }

    @Ignore("Needs working callback URL in $PROJECT_HOME/../superfeedr.properties")
    @Test
    public void testSubscribeAndRetrieveWeb() throws Exception {
        SuperFeedrSubscriber instance = null;
        type = CONTENT_TYPE.html;
        try {
            instance = SuperFeedrSubscriber.createInstance();
            instance.start();
            String id = instance.subscribeAndRetrieve(this, new URL("http://push-pub.appspot.com#article.h-entry"), null);
            assertNotNull(htmlContent);
            assertNotNull(jsonContent);
            // Turns out that web content with 'retrieve' is *only* delivered via pubsubhubbub (not in HTTP response)
            // We end up with 2 content items returned, first is HTML content delivered via the callback, second
            // is a JSON status object returned in the HTTP response). Need a specific handler for this.
            assertNotNull(instance.subs.get(id));
            assertNotNull(instance.handlers.get(id));
        } finally {
            if (instance != null) {instance.stop(true);}
        }
    }

    @Ignore("Needs working callback URL in $PROJECT_HOME/../superfeedr.properties")
    @Test
    public void testSubscribeAndRetrieveWebWait() throws Exception {
        SuperFeedrSubscriber instance = null;
        type = CONTENT_TYPE.html;
        try {
            instance = SuperFeedrSubscriber.createInstance();
            instance.start();
            String id = instance.subscribeAndRetrieve(this, new URL("http://push-pub.appspot.com#article.h-entry"), null);
            assertNotNull(htmlContent);
            assertNotNull(jsonContent);
            // Turns out that web content with 'retrieve' is *only* delivered via pubsubhubbub (not in HTTP response)
            // We end up with 2 content items returned, first is HTML content delivered via the callback, second
            // is a JSON status object returned in the HTTP response). Need a specific handler for this.
            assertNotNull(instance.subs.get(id));
            assertNotNull(instance.handlers.get(id));
            org.jsoup.nodes.Document prev = htmlContent;
            waitForPublish();
            assertThat(htmlContent, not(equalTo(prev)));
        } finally {
            if (instance != null) {instance.stop(true);}
        }
    }

    @Ignore("Needs working callback URL in $PROJECT_HOME/../superfeedr.properties")
    @Test
    public void testSubscribeAndRetrieveWebAtom() throws Exception {
        SuperFeedrSubscriber instance = null;
        type = CONTENT_TYPE.html;
        try {
            instance = SuperFeedrSubscriber.createInstance();
            instance.getOptions().put(SuperFeedrSubscriber.PROPS_FORMAT, "atom");
            instance.start();
            String id = instance.subscribeAndRetrieve(this, new URL("http://push-pub.appspot.com#article.h-entry"), null);
            assertNotNull(xmlContent);
            assertNotNull(htmlContent);
            // Turns out that web content with 'retrieve' is *only* delivered via pubsubhubbub (not in HTTP response)
            // We end up with 2 content items returned, first is HTML content delivered via the callback, second
            // is an XML status object returned in the HTTP response). Need a specific handler for this.
            assertNotNull(instance.subs.get(id));
            assertNotNull(instance.handlers.get(id));
        } finally {
            if (instance != null) {instance.stop(true);}
        }
    }

    private void waitForPublish() {
        try {
            System.out.println("Now go to http://push-pub.appspot.com and publish something");
            if (type == CONTENT_TYPE.html) {
                System.out.println("... then go make some coffee or something: this one takes 15 minutes");
            }
            synchronized(this) {
                this.wait();
            }
        } catch (InterruptedException exc) {
            System.out.println("Interruped waiting for a PubSubHubbub publish");
        }
    }

    public void handle(String subs_id, InputStream body, Map<String, List<String>> headers) {
        try {
            switch(type) {
                case json:
                    jsonContent = null; htmlContent = null;
                    jsonContent = new JSONObject(new JSONTokener(body));
                    hubContent = jsonContent.toString(2);
                    break;
                case atom:
                    jsonContent = null; htmlContent = null;
                    xmlContent = builder.parse(body);
                    hubContent = SuperFeedrAtomChannel.prettyPrint(xmlContent, xmlContent);
                    break;
                case html:
                    // For html, we could have any of xml, json or html because a feed status
                    // entry is returned when we use the 'retrieve' verb.
                    hubContent = readStream(body);
                    System.out.println("Raw content:" + hubContent);
                    if (JSON_START.matcher(hubContent).find()) {
                        System.out.println("Have JSON content from HTMl subscription");
                        jsonContent = new JSONObject(hubContent);
                        hubContent = jsonContent.toString(2);
                    } else if (XML_START.matcher(hubContent).find()){
                        System.out.println("Have XML content from HTMl subscription");
                        xmlContent = builder.parse(new ByteArrayInputStream(hubContent.getBytes()));
                        hubContent = SuperFeedrAtomChannel.prettyPrint(xmlContent, xmlContent);
                    } else {
                        System.out.println("Have HTML content from HTMl subscription");
                        htmlContent = Jsoup.parseBodyFragment(hubContent);
                        hubContent = htmlContent.outerHtml();
                    }
                    break;
            }
            System.out.println("Parsed content:" + hubContent);
            synchronized(this) {
                this.notify();
            }
        } catch (Exception exc) {
            System.out.println("Error handling content: " + exc.getMessage());
            notifyFailed = exc;
        }
    }

    private String readStream(InputStream in) throws IOException {
        Scanner scanner = new Scanner(in);
        StringBuffer result = new StringBuffer();
        while (scanner.hasNextLine()) {
            result.append(scanner.nextLine());
        }
        scanner.close();
        return result.toString();
    }
}
