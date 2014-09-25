package com.eventswarm.social.channels;

import com.eventswarm.events.Event;
import com.eventswarm.events.jdo.OrgJsonPart;
import com.eventswarm.events.jdo.OrgJsonEvent;
import com.eventswarm.social.events.JsonTweetEvent;
import org.apache.log4j.Logger;
import twitter4j.Status;
import com.eventswarm.channels.JsonChannel;
import com.eventswarm.channels.JsonEventFactory;
import twitter4j.StatusListener;
import twitter4j.TwitterStreamFactory;

/**
 * Use JSON classes rather than the Twitter4J opaque classes for status tweets so we get more control, better re-use
 * and the ability to persist the raw JSON whenever required.
 *
 * Just overrides the onStatus method at this stage.
 *
 * Getting the raw JSON out of a tweet requires that the configuration parameter twitter4j.jsonStoreEnabled is set to
 * true. This can be done either in twitter4j.properties or in the configuration associated with the
 * TwitterStreamFactory passed to the constructor.
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 24/09/13
 * Time: 2:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class JsonStatusListenerChannel extends StatusListenerChannel implements StatusListener {

    private static Logger log = Logger.getLogger(JsonStatusListenerChannel.class);

    public JsonStatusListenerChannel() {
        super();
    }

    public JsonStatusListenerChannel(TwitterStreamFactory factory) {
        super(factory);
    }

    public JsonStatusListenerChannel(long[] follow, String[] track) {
        super(follow, track);
    }

    public JsonStatusListenerChannel(TwitterStreamFactory factory, long[] follow, String[] track) {
        super(factory, follow, track);
    }

    @Override
    public void onStatus(Status status) {
        Event event = new JsonTweetEvent(status);
        this.fire(event);
    }
}
