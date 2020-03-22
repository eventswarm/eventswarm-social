package com.eventswarm.social.helpers;

import com.eventswarm.AddEventAction;
import com.eventswarm.AddEventTrigger;
import com.eventswarm.events.Event;
import com.eventswarm.social.events.JsonTweetEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a file containing JSON status items and returns TweetEvents
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 3/10/13
 * Time: 5:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class JsonEventReader implements AddEventAction {
    List<Event> events = new ArrayList<Event>();

    public JsonTweetEvent getFirstEvent(InputStream stream) throws Exception {
        events.clear();
        ReplayJSON player = new ReplayJSON(new InputStreamReader(stream), (AddEventAction) this);
        player.process();
        return (JsonTweetEvent) events.get(0);
    }

    public JsonTweetEvent getLastEvent(InputStream stream) throws Exception {
        events.clear();
        ReplayJSON player = new ReplayJSON(new InputStreamReader(stream), (AddEventAction) this);
        player.process();
        return (JsonTweetEvent) events.get(events.size()-1);
    }

    public void execute(AddEventTrigger trigger, Event event) {
        events.add(event);
    }
}
