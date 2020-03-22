package com.eventswarm.social.helpers;

import com.eventswarm.AddEventAction;
import com.eventswarm.AddEventTrigger;
import com.eventswarm.events.Event;
import com.eventswarm.social.channels.StatusListenerChannel;
import com.eventswarm.social.events.JsonTweetEvent;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

import twitter4j.Status;
import twitter4j.StatusListener;

import java.io.InputStreamReader;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 1/10/13
 * Time: 1:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReplayJSONTest extends StatusListenerChannel implements AddEventAction {
    private Status tweet;
    private Event event;
    private int count = 0;

    @Test
    public void verifyTweets() throws Exception {
        ReplayJSON instance = new ReplayJSON(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("fixtures/captured.json")), (StatusListener) this);
        instance.process();
    }

    @Test
    public void verifyEvents() throws Exception {
        ReplayJSON instance = new ReplayJSON(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("fixtures/captured.json")), (AddEventAction) this);
        instance.process();
    }


    @Override
    public void onStatus(Status tweet) {
        synchronized (this) {
            count++;
            System.out.println("Verifying status " + Integer.toString(count));
            this.tweet = tweet;
            assertNotNull(tweet);
            assertNotNull(tweet.getId());
            assertNotNull(tweet.getId());
            assertNotNull(tweet.getText());
            assertNotNull(tweet.getCreatedAt());
            assertNotNull(tweet.getUser().getScreenName());
            /* The createStatus method in twitter4j doesn't enable this method */
            // assertNotNull(TwitterObjectFactory.getRawJSON(tweet));
            this.notify();
        }
    }

    public void execute(AddEventTrigger addEventTrigger, Event event) {
        synchronized (this) {
            count++;
            System.out.println("Verifying JsonTweetEvent " + Integer.toString(count));
            this.event = event;
            assertNotNull(event.getHeader().getTimestamp());
            assertNotNull(event.getHeader().getEventId());
            assertEquals("twitter.com", event.getHeader().getSource().getSourceId());
            assertNotNull(((JsonTweetEvent)event).getKeywords());
            assertNotNull(((JsonTweetEvent)event).getStatusText());
            this.notify();
        }
    }
}
