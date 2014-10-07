package com.eventswarm.social;

import com.eventswarm.events.Event;
import com.eventswarm.powerset.EventKey;
import com.eventswarm.social.events.TweetEntities;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class AuthorEventKey implements EventKey<String> {
    public String getKey(Event event) {
        if (TweetEntities.class.isInstance(event)) {
            return "@" + ((TweetEntities) event).getAuthor();
        } else return null;
    }
}
