package com.eventswarm.social.channels;

import com.eventswarm.AddEventTrigger;
import com.eventswarm.channels.AbstractChannel;
import com.eventswarm.events.Event;
import com.eventswarm.social.events.JsonTweetEvent;
import twitter4j.*;

/**
 * Channel to encapsulate a twitter query as an stream of EventSwarm events
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 24/07/13
 * Time: 11:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class JsonStatusQueryChannel extends StatusQueryChannel {
    public JsonStatusQueryChannel(String query) {
        super(query);
    }

    public JsonStatusQueryChannel(TwitterFactory factory, String query) {
        super(factory, query);
    }

    public JsonStatusQueryChannel(Twitter twitter, String query) {
        super(twitter, query);
    }

    @Override
    public Event next() throws Exception {
        if (iterator.hasNext()) {
            return new JsonTweetEvent(iterator.next());
        } else {
            this.stop();
            return null;
        }
    }
}
