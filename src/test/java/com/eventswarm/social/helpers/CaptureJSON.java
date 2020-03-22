package com.eventswarm.social.helpers;

import com.eventswarm.social.channels.StatusListenerChannel;
import org.apache.log4j.Logger;
import twitter4j.Status;
import twitter4j.TwitterObjectFactory;
import twitter4j.TwitterStreamFactory;


/**
 * Capture the raw JSON data coming from twitter and write it to stdout
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 24/09/13
 * Time: 2:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class CaptureJSON extends StatusListenerChannel {

    private static Logger log = Logger.getLogger(CaptureJSON.class);
    private boolean stop = false;

    public CaptureJSON() {
        super();
    }

    public CaptureJSON(TwitterStreamFactory factory) {
        super(factory);
    }

    public CaptureJSON(long[] follow, String[] track) {
        super(follow, track);
    }

    public CaptureJSON(TwitterStreamFactory factory, long[] follow, String[] track) {
        super(factory, follow, track);
    }

    @Override
    public void onStatus(Status status) {
        if (!stop) System.out.println(TwitterObjectFactory.getRawJSON(status));
    }

    public void stop() {
        this.stop = true;
    }

    public static void main(String[] argv) {
        long[] follow = {};
        final CaptureJSON instance = new CaptureJSON(follow, argv);
        instance.connect();
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                instance.stop();
            }
        });
    }
}
