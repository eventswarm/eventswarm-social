package com.eventswarm.social.channels;

import com.eventswarm.AddEventAction;
import com.eventswarm.AddEventTrigger;
import com.eventswarm.events.Event;
import com.eventswarm.events.XmlEvent;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class SuperFeedrAtomChannelTest implements AddEventAction {
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
        SuperFeedrAtomChannel instance = new SuperFeedrAtomChannel(subscriber);
        instance.registerAction(this);
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("fixtures/superfeedr.xml");
        instance.handle("1234", stream, null);
        assertEquals(2, events.size());
        assertEquals("http://push-pub.appspot.com/feed/5723348596162560", events.get(0).getHeader().getEventId());
        assertEquals("push-pub.appspot.com", events.get(0).getHeader().getSource().getSourceId());
        Calendar timestamp = new GregorianCalendar();
        timestamp.setTime(events.get(0).getHeader().getTimestamp());
        assertEquals(2014, timestamp.get(Calendar.YEAR));
        assertEquals(9, timestamp.get(Calendar.MONTH));
        assertEquals(3, timestamp.get(Calendar.DAY_OF_MONTH));
        assertEquals(16, timestamp.get(Calendar.HOUR_OF_DAY));
        assertEquals(13, timestamp.get(Calendar.MINUTE));
        assertEquals(58, timestamp.get(Calendar.SECOND));
        assertEquals("http://push-pub.appspot.com/feed/5723348596162560", ((XmlEvent) events.get(0)).getString("id"));
        assertEquals("http://push-pub.appspot.com/feed/5678701068943360", events.get(1).getHeader().getEventId());
        assertEquals("push-pub.appspot.com", events.get(1).getHeader().getSource().getSourceId());
        assertEquals("hello", ((XmlEvent) events.get(1)).getString("title"));
    }


    @Test
    public void testUnsubscribe() throws Exception {
        SuperFeedrAtomChannel instance = new SuperFeedrAtomChannel(subscriber);
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
        SuperFeedrAtomChannel instance = new SuperFeedrAtomChannel(subscriber);
        instance.registerAction(this);
        instance.subscribe(topic, false);
        assertTrue(instance.getSubscriptions().containsValue(topic));
        assertTrue(instance.getSubscriber().subs.containsValue(topic));
        assertTrue(instance.getSubscriber().handlers.containsValue(instance));
        instance.unsubscribe();
    }

    @Test
    public void testSubscribeWithBackfill() throws Exception {
        SuperFeedrAtomChannel instance = new SuperFeedrAtomChannel(subscriber);
        instance.registerAction(this);
        instance.subscribe(topic, true);
        assertTrue(instance.getSubscriptions().containsValue(topic));
        assertTrue(instance.getSubscriber().subs.containsValue(topic));
        assertTrue(instance.getSubscriber().handlers.containsValue(instance));
        assertFalse("Should be some events", events.isEmpty());
        System.out.println("Successfully processed " + Long.toString(instance.getCount()) + " events");
        assertEquals(0L, instance.getErrors());
        assertNotNull("Event should have an id", events.get(0).getHeader().getEventId());
        assertNotNull("Event should have a source", events.get(0).getHeader().getSource().getSourceId());
        assertNotNull("Event should have a timestamp", events.get(0).getHeader().getTimestamp().getTime());
        assertNotNull("JSON should contain an id field", ((XmlEvent) events.get(0)).getString("id"));
        instance.unsubscribe();
    }

    @Test
    public void testSubscribeGoogleNews() throws Exception {
        SuperFeedrAtomChannel instance = new SuperFeedrAtomChannel(subscriber);
        instance.registerAction(this);
        topic = new URL("http://news.google.com/news?ie=UTF-8&output=rss&q=" + URLEncoder.encode("asx bhp", "UTF8"));
        instance.subscribe(topic, true);
        assertFalse("Should be some events", events.isEmpty());
        System.out.println("Successfully processed " + Long.toString(instance.getCount()) + " events");
        assertEquals(0L, instance.getErrors());
        XmlEvent event0 = (XmlEvent) events.get(0);
        System.out.println("First event id: " + event0.getString("id"));
        System.out.println("First event title: " + event0.getString("title"));
        System.out.println("First event summary: " + event0.getString("summary"));
        assertNotNull("Event should have an id", events.get(0).getHeader().getEventId());
        assertNotNull("Event should have a source", events.get(0).getHeader().getSource().getSourceId());
        assertNotNull("Event should have a timestamp", events.get(0).getHeader().getTimestamp().getTime());
        assertNotNull("XML should contain an id field", event0.getString("id"));
        instance.unsubscribe();
    }

    public void execute(AddEventTrigger trigger, Event event) {
        events.add(event);
    }
}
