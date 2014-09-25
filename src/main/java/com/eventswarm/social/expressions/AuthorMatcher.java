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
public class AuthorMatcher implements Matcher {

    private String author = null;

    private AuthorMatcher() {
        super();
    }

    /**
     * Initialise this object with the screen name of the author to match, folding to lower case
     * 
     * @param author
     */
    public AuthorMatcher(String author) {
        super();
        this.setAuthor(author);
    }

    /**
     * Returns true if the event is a TweetEvent whose author screen name
     * matches this objects author.
     * 
     * @param event
     * @return
     */
    public boolean matches(Event event) {
        if (!TweetEntities.class.isInstance(event)) return false;
        return (((TweetEntities) event).getAuthor().toLowerCase().equals(author));
    }

    /**
     * Return the author, without a leading '@'
     * 
     * @return
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Set this object's author, stripping the leading '@' if present and folding to lower case
     * 
     * @param author
     */
    private void setAuthor(String author) {
        if (author.charAt(0) == '@') {
            this.author = author.substring(1).toLowerCase();
        } else {
            this.author = author.toLowerCase();
        }
    }
}
