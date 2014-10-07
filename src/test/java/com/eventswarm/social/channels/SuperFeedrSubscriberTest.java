package com.eventswarm.social.channels;

import org.apache.log4j.BasicConfigurator;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.beans.XMLDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 6/10/2014
 * Time: 2:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class SuperFeedrSubscriberTest implements PubSubContentHandler {
    private String hubContent;
    private Exception notifyFailed;

    @BeforeClass
    public static void setup() throws Exception {
        BasicConfigurator.configure();
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

    @Test
    public void testSubscribe() throws Exception {
        SuperFeedrSubscriber instance = null;
        try {
            instance = SuperFeedrSubscriber.createInstance();
            instance.start();
            String id = instance.subscribe(this, new URL("http://push-pub.appspot.com/feed"));
            assertNotNull("ID for subscription should be returned", id);
            Thread.sleep(4000);
            assertNotNull("Subscription should be confirmed", instance.confirmed.get(id));
            assertTrue("Confirmation flag should be true",instance.confirmed.get(id));
        } finally {
            if (instance != null) {instance.stop(true);}
        }
    }

    @Test
    public void testSubscribeAndRetrieveJson() throws Exception {
        SuperFeedrSubscriber instance = null;
        try {
            instance = SuperFeedrSubscriber.createInstance();
            instance.start();
            String id = instance.subscribeAndRetrieve(this, new URL("http://push-pub.appspot.com/feed"), null);
            assertNotNull(hubContent);
            // make sure we can parse it as JSON
            new JSONObject(hubContent);
            assertNotNull(instance.subs.get(id));
            assertNotNull(instance.handlers.get(id));
        } finally {
            if (instance != null) {instance.stop(true);}
        }
    }

    @Test
    public void testSubscribeAndRetrieveAtom() throws Exception {
        SuperFeedrSubscriber instance = null;
        try {
            instance = SuperFeedrSubscriber.createInstance();
            instance.start();
            instance.getOptions().put(SuperFeedrSubscriber.PROPS_FORMAT, "atom");
            String id = instance.subscribeAndRetrieve(this, new URL("http://push-pub.appspot.com/feed"), null);
            assertNotNull(hubContent);
            // TODO: make sure we can parse it as XML
            assertNotNull(instance.subs.get(id));
            assertNotNull(instance.handlers.get(id));
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

    private String readStream(InputStream in) throws IOException {
        Scanner scanner = new Scanner(in);
        StringBuffer result = new StringBuffer();
        while (scanner.hasNextLine()) {
            result.append(scanner.nextLine());
        }
        return result.toString();
    }
}
