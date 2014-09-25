package com.eventswarm.social.helpers;

import com.eventswarm.AddEventAction;
import com.eventswarm.social.events.JsonTweetEvent;
import twitter4j.Status;
import twitter4j.StatusListener;
import twitter4j.TwitterObjectFactory;

import java.io.BufferedReader;
import java.io.Reader;

/**
 * Class to replay tweets from a file captured using the CaptureJSON class.
 *
 * Tweets are replayed to the StatusListener provided via the constructor.
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 1/10/13
 * Time: 1:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReplayJSON {
    private Reader input;
    private AddEventAction action;
    private StatusListener listener = null;

    public ReplayJSON(Reader input, StatusListener listener) {
        this.input = input;
        this.listener = listener;
    }

    public ReplayJSON(Reader input, AddEventAction action) {
        this.input = input;
        this.action = action;
    }

    public void process() throws Exception {
        BufferedReader reader = new BufferedReader(input);
        try {
            String line;
            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                Status tweet = TwitterObjectFactory.createStatus(line);
                if (listener != null) {
                    System.out.println("Delivering tweet to StatusListener");
                    listener.onStatus(tweet);
                } else if (action != null) {
                    System.out.println("Delivering tweet to AddEventAction");
                    action.execute(null, new JsonTweetEvent(line));
                } else {
                    throw new Exception("No listener or action to receive tweets");
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
}
