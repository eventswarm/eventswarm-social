/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eventswarm.social.expressions;
import com.eventswarm.events.Event;
import com.eventswarm.expressions.Matcher;
import com.eventswarm.social.events.TweetEntities;

/**
 * Matcher to do a case-insensitive match of mentions of user screen names on tweet events
 * 
 * @author andyb
 */
public class MentionsMatcher implements Matcher {

    private String screenName = null;

    private MentionsMatcher() {
        super();
    }

    public MentionsMatcher(String screenName) {
        super();
        this.setScreenName(screenName);
    }

    /**
     * Returns true if the event is a TweetEvent and includes a screenName
     * that matches this objects screenName, both folded to lower case.
     * 
     * @param event
     * @return
     */
    public boolean matches(Event event) {
        if (!TweetEntities.class.isInstance(event)) {
            return false;
        } else {
            return ((TweetEntities)event).getMentions().contains(this.screenName);
        }
    }

    /**
     * Return the screenName, without a leading '@' and folded to lower case
     * 
     * @return
     */
    public String getScreenName() {
        return screenName;
    }

    /**
     * Set this object's screenName, stripping the leading '@' if present and folding to lower case
     * 
     * @param screenName
     */
    private void setScreenName(String screenName) {
        if (screenName.charAt(0) == '@') {
            this.screenName = screenName.substring(1).toLowerCase();
        } else {
            this.screenName = screenName.toLowerCase();
        }
    }
}
