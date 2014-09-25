package com.eventswarm.social;

import com.eventswarm.events.Event;
import com.eventswarm.powerset.EventKeys;
import com.eventswarm.social.events.TweetEntities;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 3/12/12
 * Time: 5:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class TagEventKeys implements EventKeys<String> {
    public String[] getKeys(Event event) {
        if (TweetEntities.class.isInstance(event)) {
            Set<String> hashtags = ((TweetEntities) event).getHashtags();
            Set<String> cashtags = ((TweetEntities) event).getCashtags();
            String[] result = new String[hashtags.size()];
            int i = 0;
            for(String hashtag : hashtags) {
                result[i] = (cashtags.contains(hashtag) ? "$" : "#") + hashtag;
                i++;
            }
            return result;
        } else return null;
    }
}

