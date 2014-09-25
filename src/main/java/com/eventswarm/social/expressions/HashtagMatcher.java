/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eventswarm.social.expressions;
import com.eventswarm.events.Event;
import com.eventswarm.expressions.Matcher;
import com.eventswarm.social.events.TweetEntities;

/**
 * Matcher to match hashtags on tweet events
 * 
 * @author andyb
 */
public class HashtagMatcher implements Matcher {

    private String hashtag = null;

    private HashtagMatcher() {
        super();
    }

    public HashtagMatcher(String hashtag) {
        super();
        this.setHashtag(hashtag);
    }

    /**
     * Returns true if the event is a TweetEvent and includes a hashtag
     * that matches this objects hashtag.
     * 
     * @param event
     * @return
     */
    public boolean matches(Event event) {
        if (!TweetEntities.class.isInstance(event)) {
            return false;
        } else {
            return ((TweetEntities)event).getHashtags().contains(this.hashtag);
        }
    }

    /**
     * Return the hashtag, without a leading '#' or '$'
     * 
     * @return
     */
    public String getHashtag() {
        return hashtag;
    }

    /**
     * Set this object's hashtag, stripping the leading '#' or '$' if present and folding to lower case
     * 
     * @param hashtag
     */
    private void setHashtag(String hashtag) {
        if (hashtag.charAt(0) == '#' || hashtag.charAt(0) == '$') {
            this.hashtag = hashtag.substring(1).toLowerCase();
        } else {
            this.hashtag = hashtag.toLowerCase();
        }
    }
}
