/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eventswarm.social.expressions;
import com.eventswarm.events.Event;
import com.eventswarm.expressions.Matcher;
import com.eventswarm.social.events.TweetEntities;

/**
 * Matcher to match the twitter screen name of the author
 * 
 * @author andyb
 */
public class RetweetMatcher implements Matcher {

    public RetweetMatcher() {
        super();
    }

    /**
     * Returns true if the event is a retweet.
     * 
     * @param event
     * @return
     */
    public boolean matches(Event event) {
        if (!TweetEntities.class.isInstance(event)) return false;
        return (((TweetEntities) event).isRetweet());
    }
}
