/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eventswarm.social.channels;

import twitter4j.*;

import java.util.Set;
import java.util.HashSet;
import org.apache.log4j.Logger;

import com.eventswarm.AddEventTrigger;
import com.eventswarm.AddEventAction;
import com.eventswarm.events.Event;
import com.eventswarm.social.events.TweetEvent;


/**
 *
 * @author zoki
 */
public class StatusListenerChannel 
        implements StatusListener, AddEventTrigger
{

    private long[] follow;
    private String[] track;
    private Set<AddEventAction> actions = null;
    private TwitterStream stream;
    private int count = 0;
    private int errorCount = 0;

    // default logger
    private static Logger log = Logger.getLogger(StatusListenerChannel.class);

    /**
     * Establish a new StatusListener with no track keywords or users and the default configuration
     * from twitter4j.properties.
     */
    public StatusListenerChannel() {
        super();
        setup(new long[0], new String[0]);
    }

    /**
     * Establish a new StatusListener with no track keywords or users using the supplied factory object.
     */
    public StatusListenerChannel(TwitterStreamFactory factory) {
        super();
        setup(new long[0], new String[0]);
        this.stream = factory.getInstance();
    }

    /**
     * Create a channel to follow the specified users and track the specified hashtags/keywords using the default
     * configuration (i.e. from twitter4j.properties in classpath)
     *
     * @param follow
     * @param track
     */
    public StatusListenerChannel (long follow[], String track[]) {
        super();
        setup(follow, track);
        this.stream = new TwitterStreamFactory().getInstance();
    }

    /**
     * Create a channel to follow the specified users and track the specified hashtags/keywords using the specified
     * configuration
     *
     * @param follow
     * @param track
     */
    public StatusListenerChannel (TwitterStreamFactory factory, long follow[], String track[]) {
        super();
        setup(follow, track);
        this.stream = factory.getInstance();
    }

    /**
     * Setup shared between constructors
     *
     * @param follow
     * @param track
     */
    protected void setup(long follow[], String track[]) {
        this.follow = follow;
        this.track = track;
        this.actions = new HashSet<AddEventAction>();
    }

    @Deprecated
    public StatusListenerChannel (long follow[]) {
        // pass an empty track array
        this(follow, new String[0]);
    }

    @Deprecated
    public StatusListenerChannel (String track[]) {
        // pass an empty follow array
        this(new long[0], track);
    }
    
    public void connect(int count) {
        log.debug("Connecting to twitter");
        stream.addListener(this);
        // filter() method internally creates a thread which manipulates TwitterStream and calls these adequate listener methods continuously.
        stream.filter(new FilterQuery(count, this.follow, this.track));
    }

    /**
     * Replace the current filter parameters.
     *
     * @param follow
     * @param track
     */
    public void resetFilter(long follow[], String track[]) {
        this.follow = follow;
        this.track = track;
    }

    public void connect () {
        this.connect(0);
    }

    public void disconnect() {
        stream.shutdown();
    }

    public void registerAction(AddEventAction action) {
        this.actions.add(action);
    }

    public void unregisterAction(AddEventAction action) {
        this.actions.remove(action);
    }
    
    /**
     * Iterate through registered actions and call their AddEventAction.
     *
     * @param event
     */
    protected void fire(Event event) {
        this.count++;
        // too much debug here ... log.debug("Keywords: " + ((Keywords) event).getKeywords());
        for (AddEventAction a : actions) {
            log.debug("Delivering status tweet to listener");
            a.execute(this, event);
            log.debug("Completed delivery of status tweet to listener");
        }
    }
    
    public void onDeletionNotice(StatusDeletionNotice sdn) {
        // do nothing
        log.debug("Received deletion notice");
    }

    public void onScrubGeo(long l, long l1) {
        log.debug("Received onScrubGeo");
    }

    public void onStatus(Status status) {
        log.debug("Received status tweet");
        this.fire(new TweetEvent(status));
    }

    public void onTrackLimitationNotice(int i) {
        log.warn("Received onTrackLimitationNotice");
    }

    public void onException(Exception excptn) {
        log.error(excptn);
        this.errorCount++;
    }

    public void onStallWarning(StallWarning stallWarning) {
        log.warn(stallWarning.getMessage());
    }

    public int getCount() {
        return count;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public TwitterStream getStream() {
        return this.stream;
    }

}
