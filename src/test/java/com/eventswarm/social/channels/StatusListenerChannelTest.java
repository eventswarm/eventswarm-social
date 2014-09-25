/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eventswarm.social.channels;

import com.eventswarm.AddEventAction;
import com.eventswarm.AddEventTrigger;
import com.eventswarm.events.Event;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import org.apache.log4j.*;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 *
 * @author zoki
 */
public class StatusListenerChannelTest implements AddEventAction {

    final ArrayList events = new ArrayList();
    TwitterStreamFactory factory;

    public StatusListenerChannelTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        // create a console appender so we can collect log output
        BasicConfigurator.configure();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        ConfigurationBuilder conf = new ConfigurationBuilder();
        Properties props = new Properties();
        props.load(this.getClass().getClassLoader().getResourceAsStream("twitter4j.properties"));

        conf.setOAuthAccessToken((String) props.get("oauth.accessToken"))
                .setOAuthAccessTokenSecret((String) props.get("oauth.accessTokenSecret"))
                .setOAuthConsumerKey((String) props.get("oauth.consumerKey"))
                .setOAuthConsumerSecret((String) props.get("oauth.consumerSecret"));
        factory = new TwitterStreamFactory(conf.build());
    }

    @After
    public void tearDown() {
    }

    /**
     * Create with configuration object
     */
    @Test
    public void testConstructConf () {
        System.out.println("Construct with configuration object");
        StatusListenerChannel instance = new StatusListenerChannel(factory, new long[0], new String[0]);
        instance.connect();
        instance.disconnect();
    }

    /**
     * Test of connect method, of class StatusListenerChannel.
     */
    @Test
    public void testConnect0() {
        System.out.println("connect with no follows or tracks");
        StatusListenerChannel instance = new StatusListenerChannel(new long[0], new String[0]);
        instance.connect();
        instance.disconnect();
        // if we get here without failing, then we're OK
    }

    /**
     * Test of connect method, of class StatusListenerChannel.
     */
    @Test
    public void testConnect1() {
        System.out.println("connect with a track");
        String[] track = {"#qanda"};
        for (int i = 0; i < track.length; i++)
            {System.out.println(track[i]);
        }

        StatusListenerChannel instance = new StatusListenerChannel(track);
        instance.connect();
        System.out.println("connected with a track only ");
        // if we get here without failing, then we're OK
        instance.disconnect();
   }

    /**
     * Test of disconnect method, of class StatusListenerChannel.
     */
    @Test
    public void testDisconnect() {
        System.out.println("disconnect");
        StatusListenerChannel instance = new StatusListenerChannel(new long[0], new String[0]);
        instance.connect();
        instance.disconnect();
        // If we get here, we're OK
    }

    /**
     * Test of registerAction method, of class StatusListenerChannel.
     */
    @Test
    public void testRegisterAction() {
        System.out.println("registerAction");
        AddEventAction action = this;
        StatusListenerChannel instance = new StatusListenerChannel(new long[0], new String[0]);
        instance.registerAction(action);
        instance.unregisterAction(action);
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of unregisterAction method, of class StatusListenerChannel.
     */
    @Test
    public void testUnregisterAction() {
        System.out.println("unregisterAction");
        AddEventAction action = this;
        StatusListenerChannel instance = new StatusListenerChannel(new long[0], new String[0]);
        instance.registerAction(action);
        instance.unregisterAction(action);
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * Test of onDeletionNotice method, of class StatusListenerChannel.
     */
//    @Test
//    public void testOnDeletionNotice() {
//        System.out.println("onDeletionNotice");
//        StatusDeletionNotice sdn = null;
//        StatusListenerChannel instance = new StatusListenerChannel(new long[0], new String[0]);
//        instance.onDeletionNotice(sdn);
//        // TODO review the generated test code and remove the default call to fail.        fail("The test case is a prototype.");
//    }

    /**
     * Test of onScrubGeo method, of class StatusListenerChannel.
     */
//    @Test
//    public void testOnScrubGeo() {
//        System.out.println("onScrubGeo");
//        long l = 0L;
//        long l1 = 0L;
//        StatusListenerChannel instance = new StatusListenerChannel(new long[0], new String[0]);
//        instance.onScrubGeo(l, l1);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }

    /**
     * Test of onStatus method, of class StatusListenerChannel.
     */
    @Test
    public void testOnStatus() {
        System.out.println("onStatus");
        AddEventAction action = this;
        String[] track = {"#news"};
        // just check the character string through printing
        for (int i = 0; i < track.length; i++)
            {System.out.println(track[i]);
        }
        StatusListenerChannel instance = new StatusListenerChannel(new long[0], track);
        System.out.println ("after StatusListenerChannel creation");
        instance.connect();
        instance.registerAction(action);
      
        System.out.println ("after instance.connect ()");
        try {
            synchronized (events) {
                events.wait();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(StatusListenerChannelTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        // TODO review the generated test code and remove the default call to fail.
        System.out.println ("before assertTrue");
        assertTrue(this.events.size() >= 1);
        instance.disconnect();
    }

    /**
     * Test of onTrackLimitationNotice method, of class StatusListenerChannel.
     */
//    @Test
//    public void testOnTrackLimitationNotice() {
//        System.out.println("onTrackLimitationNotice");
//        int i = 0;
//        StatusListenerChannel instance = null;
//        instance.onTrackLimitationNotice(i);
//        // TODO review the generated test code and remove the default call to fail.
//    }

    /**
     * Test of onException method, of class StatusListenerChannel.
     */
//    @Test
//    public void testOnException() {
//        System.out.println("onException");
//        Exception excptn = null;
//        StatusListenerChannel instance = null;
//        instance.onException(excptn);
//        // TODO review the generated test code and remove the default call to fail.
//    }

    public void execute(AddEventTrigger trigger, Event event) {
        System.out.println("Have received an event:" + event.toString());
        synchronized (events) {
            events.add(event);
            events.notify();
        }
    }

}