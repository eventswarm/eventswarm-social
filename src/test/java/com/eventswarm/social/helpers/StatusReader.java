package com.eventswarm.social.helpers;

import com.eventswarm.social.events.TweetEvent;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a file containing JSON status items and returns TweetEvents
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 3/10/13
 * Time: 5:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class StatusReader implements StatusListener {
    List<Status> statuses = new ArrayList<Status>();

    public TweetEvent getFirstEvent(InputStream stream) throws Exception {
        statuses.clear();
        ReplayJSON player = new ReplayJSON(new InputStreamReader(stream), (StatusListener) this);
        player.process();
        return new TweetEvent(statuses.get(0));
    }

    public TweetEvent getLastEvent(InputStream stream) throws Exception {
        statuses.clear();
        ReplayJSON player = new ReplayJSON(new InputStreamReader(stream), (StatusListener) this);
        player.process();
        return new TweetEvent(statuses.get(statuses.size()-1));
    }

    public void onStatus(Status status) {
        statuses.add(status);
    }

    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void onTrackLimitationNotice(int i) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void onScrubGeo(long l, long l2) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void onStallWarning(StallWarning stallWarning) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void onException(Exception e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
