package com.eventswarm.social.channels;

import com.eventswarm.AddEventAction;
import com.eventswarm.AddEventTrigger;
import com.eventswarm.DuplicateEventAction;
import com.eventswarm.DuplicateEventTrigger;
import com.eventswarm.events.Event;
import com.eventswarm.eventset.DiscreteTimeWindow;
import com.eventswarm.eventset.EventSet;
import com.eventswarm.social.events.JSoupFragmentEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;


/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class SuperFeedrHtmlChannelTest implements AddEventAction, DuplicateEventAction {
    SuperFeedrSubscriber subscriber;
    List<Event> events;
    EventSet eventSet;
    URL topic;
    Map<String,List<String>> headers;
    Map<Event,Event> duplicates;
    Date date;

    @Before
    public void setup() throws Exception {
        subscriber = SuperFeedrSubscriber.createInstance();
        events = new ArrayList<Event>();
        date = new Date(); date.setTime(date.getTime() - date.getTime()%1000); // zero milliseconds
        String[] dates = {SuperFeedrHtmlChannel.HTTP_DATE_FORMAT.format(date)};
        headers = new HashMap<String,List<String>>();
        headers.put(SuperFeedrHtmlChannel.DATE_HEADER, Arrays.asList(dates));
        eventSet = new EventSet();
        duplicates = new HashMap<Event,Event>();
    }

    @Test
    public void testConstruct() throws Exception {
        SuperFeedrHtmlChannel instance = new SuperFeedrHtmlChannel(subscriber);
        assertThat(instance.getSubscriptions(), instanceOf(Map.class));
        assertThat(instance.getSubscriber(), instanceOf(PubSubHubbubSubscriber.class));
        assertThat(instance.getSelectors(), instanceOf(Map.class));
        assertThat(instance.getHistory(), instanceOf(DiscreteTimeWindow.class));
        assertEquals(0, instance.getCount());
        assertEquals(0, instance.getErrors());
    }

    @Test
    public void testConstructWithEventSet() throws Exception {
        EventSet set = new EventSet();
        SuperFeedrHtmlChannel instance = new SuperFeedrHtmlChannel(subscriber, set);
        assertThat(instance.getSubscriptions(), instanceOf(Map.class));
        assertThat(instance.getSubscriber(), instanceOf(PubSubHubbubSubscriber.class));
        assertThat(instance.getSelectors(), instanceOf(Map.class));
        assertThat(instance.getHistory(), is(set));
        assertEquals(0, instance.getCount());
        assertEquals(0, instance.getErrors());
    }

    @Test
    public void testHandleSimple() throws Exception {
        SuperFeedrHtmlChannel instance = new SuperFeedrHtmlChannel(subscriber);
        topic = new URL("http://myfeed.com#article");
        String id = "1234";
        instance.registerAction((AddEventAction) this);
        instance.getSubscriptions().put(id, topic);
        instance.getSelectors().put(id, "article");
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("fixtures/superfeedr.html");
        instance.handle(id, stream, headers);
        assertEquals(3, events.size());
        for (Event event: events) {
            assertThat(date, is(event.getHeader().getTimestamp()));
        }
    }


    @Test
    public void testHandleDuplicates() throws Exception {
        SuperFeedrHtmlChannel instance = new SuperFeedrHtmlChannel(subscriber);
        topic = new URL("http://myfeed.com#article");
        String id = "1234";
        instance.registerAction((AddEventAction) this);
        instance.getDupeFilter().registerAction((DuplicateEventAction) this);
        instance.getSubscriptions().put(id, topic);
        instance.getSelectors().put(id, "article");
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream("fixtures/superfeedrless.html");
        instance.handle(id, stream, headers);
        assertThat(events.size(), is(2));
        assertEquals(events.size(), eventSet.size());
        for (Event event: eventSet) {checkEvent(event, date, SuperFeedrHtmlChannel.DEFAULT_SOURCE);}
        stream = this.getClass().getClassLoader().getResourceAsStream("fixtures/superfeedr.html");
        instance.handle(id, stream, headers);
        for (Event event: eventSet) {checkEvent(event, date, SuperFeedrHtmlChannel.DEFAULT_SOURCE);}
        System.out.println("first.compareTo(last) gives " + Integer.toString(((JSoupFragmentEvent) eventSet.first()).compareTo(eventSet.getEventSet().last())));
        System.out.println("last.compareTo(first) gives " + Integer.toString(((JSoupFragmentEvent) eventSet.last()).compareTo(eventSet.getEventSet().first())));
        assertEquals(events.size(), eventSet.size());
        assertEquals(events.size(), instance.getHistory().size());
        assertThat(events.size(), is(3));
        assertThat(duplicates.size(), is(2));
    }

    @Test
    public void testUnsubscribe() throws Exception {
        SuperFeedrHtmlChannel instance = new SuperFeedrHtmlChannel(subscriber);
        instance.registerAction((AddEventAction) this);
        topic = new URL("http://push-pub.appspot.com#article.h-entry");
        instance.subscribe(topic, "article.h-entry", false);
        instance.unsubscribe();
        assertTrue(instance.getSubscriptions().isEmpty());
        assertTrue(instance.getSubscriber().subs.isEmpty());
        assertTrue(instance.getSubscriber().handlers.isEmpty());
    }

    @Test
    public void testSubscribeNoBackfill() throws Exception {
        SuperFeedrHtmlChannel instance = new SuperFeedrHtmlChannel(subscriber);
        instance.registerAction((AddEventAction) this);
        topic = new URL("http://push-pub.appspot.com");
        instance.subscribe(topic, "article.h-entry", false);
        assertTrue(instance.getSubscriptions().containsValue(topic));
        assertTrue(instance.getSubscriber().subs.containsValue(topic));
        assertTrue(instance.getSubscriber().handlers.containsValue(instance));
        instance.unsubscribe();
    }

    @Ignore("Needs working callback URL in $PROJECT_HOME/../superfeedr.properties")
    @Test
    public void testSubscribeWithBackfill() throws Exception {
        SuperFeedrHtmlChannel instance = new SuperFeedrHtmlChannel(subscriber);
        instance.registerAction((AddEventAction) this);
        topic = new URL("http://push-pub.appspot.com#article.h-entry");
        instance.subscribe(topic, "article.h-entry", true);
        assertTrue(instance.getSubscriptions().containsValue(topic));
        assertTrue(instance.getSubscriber().subs.containsValue(topic));
        assertTrue(instance.getSubscriber().handlers.containsValue(instance));
        Thread.sleep(10000);
        assertFalse("Should be some events", events.isEmpty());
        assertNotNull("Event should have an id", events.get(0).getHeader().getEventId());
        assertNotNull("Event should have a source", events.get(0).getHeader().getSource().getSourceId());
        assertNotNull("Event should have a timestamp", events.get(0).getHeader().getTimestamp().getTime());
        instance.unsubscribe();
    }

    public void checkEvent(Event event, Date timestamp, String source) {
        System.out.println("Id: " + event.getHeader().getEventId() + "; Source: " + event.getHeader().getSource().getSourceId() + "; Timestamp: " + event.getHeader().getTimestamp().toString());
        assertEquals(timestamp, event.getHeader().getTimestamp());
        assertThat(event.getHeader().getSource().getSourceId(), is(SuperFeedrHtmlChannel.DEFAULT_SOURCE));
    }

    public void execute(AddEventTrigger trigger, Event event) {
        events.add(event);
        eventSet.execute(trigger, event);
    }

    public void execute(DuplicateEventTrigger trigger, Event original, Event duplicate) {
        duplicates.put(original, duplicate);
    }
}
