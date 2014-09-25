/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eventswarm.social.events;

import com.eventswarm.events.jdo.JdoEventPart;
import twitter4j.Status;


/**
 *
 * @author andyb
 */
public class TweetEventPart extends JdoEventPart {

    Status status;

    // Default constructor only for persistence
    protected TweetEventPart() {
        super();
    }

    public TweetEventPart(Status status) {
        super();
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
