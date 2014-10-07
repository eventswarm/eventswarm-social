package com.eventswarm.social.channels;

import com.eventswarm.AddEventAction;
import com.eventswarm.AddEventTrigger;
import com.eventswarm.events.Event;
import org.junit.Test;
import static org.junit.Assert.*;


import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class JsonStatusListenerChannelTest implements AddEventAction {
    private static String[] TRACK_NEWS = {"#news"};
    private static long[] FOLLOW_NONE = {};
    private List<Event> events = new LinkedList<Event>();

    @Test
    public void printTweetJson() throws Exception {
        JsonStatusListenerChannel instance = new JsonStatusListenerChannel(FOLLOW_NONE, TRACK_NEWS);
        instance.registerAction(this);
        instance.connect();
        System.out.println("Waiting for tweets");
        synchronized(this) {
            wait();
            instance.disconnect();
        }
        assertEquals(1, events.size());
        for (Event event : events) {
            System.out.println(event.toString());
        }
    }

    @Override
    public void execute(AddEventTrigger addEventTrigger, Event event) {
        System.out.println("Tweet delivered");
        synchronized(this) {
            events.add(event);
            notify();
        }
    }

// TODO: find a way to make replayed tweets behave like real tweets (can't get JSON string from them)
//    @Test
//    public void replayJsonFromFile() throws Exception {
//        JsonStatusListenerChannel instance = new JsonStatusListenerChannel();
//        instance.registerAction(this);
//        ReplayJSON player = new ReplayJSON(new FileReader("fixtures/captured.json"), instance);
//        player.process();
//        assertEquals(11, events.size());
//    }
}
