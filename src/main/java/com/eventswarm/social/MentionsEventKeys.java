package com.eventswarm.social;

import com.eventswarm.events.Event;
import com.eventswarm.powerset.EventKeys;
import com.eventswarm.social.events.TweetEntities;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */

public class MentionsEventKeys implements EventKeys<String> {
    public String[] getKeys(Event event) {
        if (TweetEntities.class.isInstance(event)) {
            Set<String> mentions = ((TweetEntities) event).getMentions();
            String[] result = new String[mentions.size()];
            int i = 0;
            for(String mention : mentions) {
                result[i] = "@" + mention;
                i++;
            }
            return result;
        } else return null;
    }
}
