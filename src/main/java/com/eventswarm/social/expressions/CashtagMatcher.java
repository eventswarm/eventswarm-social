/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eventswarm.social.expressions;
import com.eventswarm.events.Event;
import com.eventswarm.expressions.Matcher;
import com.eventswarm.social.events.TweetEntities;

/**
 * Matcher to match cashtags on tweet events
 * 
 * @author andyb
 */
public class CashtagMatcher implements Matcher {

    private String cashtag = null;

    private CashtagMatcher() {
        super();
    }

    public CashtagMatcher(String cashtag) {
        super();
        this.setHashtag(cashtag);
    }

    /**
     * Returns true if the event is a TweetEvent and includes a cashtag
     * that matches this objects cashtag.
     * 
     * @param event
     * @return
     */
    public boolean matches(Event event) {
        if (!TweetEntities.class.isInstance(event)) {
            return false;
        } else {
            return ((TweetEntities)event).getCashtags().contains(this.cashtag);
        }
    }

    /**
     * Return the cashtag, without a leading '$'
     * 
     * @return
     */
    public String getHashtag() {
        return cashtag;
    }

    /**
     * Set this object's cashtag, stripping the leading '$' if present and folding to lower case
     * 
     * @param cashtag
     */
    private void setHashtag(String cashtag) {
        if (cashtag.charAt(0) == '$') {
            this.cashtag = cashtag.substring(1).toLowerCase();
        } else {
            this.cashtag = cashtag.toLowerCase();
        }
    }
}
