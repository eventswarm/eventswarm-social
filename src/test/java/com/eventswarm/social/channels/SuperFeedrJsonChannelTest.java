package com.eventswarm.social.channels;

import com.eventswarm.AddEventAction;
import com.eventswarm.AddEventTrigger;
import com.eventswarm.events.Event;
import com.eventswarm.events.jdo.OrgJsonEvent;
import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 7/10/2014
 * Time: 11:11 AM
 * To change this template use File | Settings | File Templates.
 */
public class SuperFeedrJsonChannelTest implements AddEventAction {
    SuperFeedrSubscriber subscriber;
    List<Event> events;
    URL topic;

    @BeforeClass
    public static void setupLogging() throws Exception {
        BasicConfigurator.configure();
    }

    @Before
    public void setup() throws Exception {
        subscriber = SuperFeedrSubscriber.createInstance();
        events = new ArrayList<Event>();
        topic = new URL("http://push-pub.appspot.com/feed");
    }

    @Test
    public void testHandleSimple() throws Exception {
        SuperFeedrJsonChannel instance = new SuperFeedrJsonChannel(null);
        instance.registerAction(this);
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("fixtures/superfeedr.json");
        instance.handle(stream, null);
        assertEquals(2, events.size());
        assertEquals("domain.tld:09/05/03-1", events.get(0).getHeader().getEventId());
        assertEquals("domain.tld", events.get(0).getHeader().getSource().getSourceId());
        assertEquals(1271851240000L, events.get(0).getHeader().getTimestamp().getTime());
        assertEquals("domain.tld:09/05/03-1", ((OrgJsonEvent) events.get(0)).getString("id"));
        assertEquals("http://www.macrumors.com/2009/05/06/adwhirl-free-ad-supported-iphone-apps-can-very-lucrative/", events.get(1).getHeader().getEventId());
        assertEquals("domain.tld", events.get(1).getHeader().getSource().getSourceId());
        assertEquals(1241616887000L, events.get(1).getHeader().getTimestamp().getTime());
        assertEquals("AdWhirl: Free Ad-Supported iPhone Apps Can Be Very Lucrative", ((OrgJsonEvent) events.get(1)).getString("title"));
    }


    @Test
    public void testUnsubscribe() throws Exception {
        SuperFeedrJsonChannel instance = new SuperFeedrJsonChannel(subscriber);
        instance.registerAction(this);
        instance.subscribe(topic, false);
        instance.subscribe(new URL("http://blog.superfeedr.com"), false);
        instance.unsubscribe();
        assertTrue(instance.getSubscriptions().isEmpty());
        assertTrue(instance.getSubscriber().subs.isEmpty());
        assertTrue(instance.getSubscriber().handlers.isEmpty());
    }

    @Test
    public void testSubscribeNoBackfill() throws Exception {
        SuperFeedrJsonChannel instance = new SuperFeedrJsonChannel(subscriber);
        instance.registerAction(this);
        instance.subscribe(topic, false);
        assertTrue(instance.getSubscriptions().containsValue(topic));
        assertTrue(instance.getSubscriber().subs.containsValue(topic));
        assertTrue(instance.getSubscriber().handlers.containsValue(instance));
        instance.unsubscribe();
    }

    @Test
    public void testSubscribeWithBackfill() throws Exception {
        SuperFeedrJsonChannel instance = new SuperFeedrJsonChannel(subscriber);
        instance.registerAction(this);
        instance.subscribe(topic, true);
        assertTrue(instance.getSubscriptions().containsValue(topic));
        assertTrue(instance.getSubscriber().subs.containsValue(topic));
        assertTrue(instance.getSubscriber().handlers.containsValue(instance));
        assertFalse("Should be some events", events.isEmpty());
        assertNotNull("Event should have an id", events.get(0).getHeader().getEventId());
        assertNotNull("Event should have a source", events.get(0).getHeader().getSource().getSourceId());
        assertNotNull("Event should have a timestamp", events.get(0).getHeader().getTimestamp().getTime());
        assertNotNull("JSON should contain an id field", ((OrgJsonEvent) events.get(0)).getString("id"));
        instance.unsubscribe();
    }

    @Override
    public void execute(AddEventTrigger trigger, Event event) {
        events.add(event);
    }
}
