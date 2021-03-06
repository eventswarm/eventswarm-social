package com.eventswarm.social.channels;

import com.eventswarm.AddEventAction;
import com.eventswarm.AddEventTrigger;
import com.eventswarm.events.Event;
import com.eventswarm.events.jdo.OrgJsonEvent;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class SuperFeedrJsonChannelTest implements AddEventAction {
    SuperFeedrSubscriber subscriber;
    List<Event> events;
    URL topic;

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
        instance.handle("1234", stream, null);
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

    @Test
    public void testSubscribeGoogleNews() throws Exception {
        SuperFeedrJsonChannel instance = new SuperFeedrJsonChannel(subscriber);
        instance.registerAction(this);
        topic = new URL("http://news.google.com/news?ie=UTF-8&output=rss&q=" + URLEncoder.encode("asx bhp", "UTF8"));
        instance.subscribe(topic, true);
        assertFalse("Should be some events", events.isEmpty());
        OrgJsonEvent event0 = (OrgJsonEvent) events.get(0);
        System.out.println("First event id: " + event0.getString("id"));
        System.out.println("First event title: " + event0.getString("title"));
        System.out.println("First event json: " + event0.getJsonString());
        assertNotNull("Event should have an id", events.get(0).getHeader().getEventId());
        assertNotNull("Event should have a source", events.get(0).getHeader().getSource().getSourceId());
        assertNotNull("Event should have a timestamp", events.get(0).getHeader().getTimestamp().getTime());
        assertNotNull("JSON should contain an id field", event0.getString("id"));
        instance.unsubscribe();
    }

    public void execute(AddEventTrigger trigger, Event event) {
        events.add(event);
    }
}
