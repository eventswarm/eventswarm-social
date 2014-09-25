package com.eventswarm.social.channels;

import com.eventswarm.AddEventAction;
import com.eventswarm.AddEventTrigger;
import com.eventswarm.events.Event;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 24/07/13
 * Time: 12:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class StatusQueryChannelTest implements AddEventAction {
    final ArrayList events = new ArrayList();
    TwitterFactory factory;

    @Before
    public void setUp() throws Exception {
        ConfigurationBuilder conf = new ConfigurationBuilder();
        Properties props = new Properties();
        props.load(this.getClass().getClassLoader().getResourceAsStream("twitter4j.properties"));
        conf.setOAuthAccessToken((String) props.get("oauth.accessToken"))
                .setOAuthAccessTokenSecret((String) props.get("oauth.accessTokenSecret"))
                .setOAuthConsumerKey((String) props.get("oauth.consumerKey"))
                .setOAuthConsumerSecret((String) props.get("oauth.consumerSecret"));
        factory = new TwitterFactory(conf.build());
    }

    /**
     * Create without configuration object
     */
    @Test
    public void construct() throws Exception {
        System.out.println("Construct with default factory");
        StatusQueryChannel instance = new StatusQueryChannel("#eventswarm");
        assertNotNull(instance.getQuery());
        assertNotNull(instance.getTwitter());
    }

    /**
     * Create with configuration object
     */
    @Test
    public void constructWithConf() throws Exception {
        System.out.println("Construct with supplied factory");
        StatusQueryChannel instance = new StatusQueryChannel(factory, "#eventswarm");
        assertNotNull(instance.getQuery());
        assertNotNull(instance.getTwitter());
    }

    /**
     * Create with already-created twitter object
     */
    @Test
    public void constructWithInstance() throws Exception {
        System.out.println("Construct with supplied instance");
        StatusQueryChannel instance = new StatusQueryChannel(factory.getInstance(), "#eventswarm");
        assertNotNull(instance.getQuery());
        assertNotNull(instance.getTwitter());
    }

    /**
     * Setup (run) query
     */
    @Test
    public void setup() throws Exception {
        System.out.println("Run query '#eventswarm'");
        StatusQueryChannel instance = new StatusQueryChannel(factory.getInstance(), "#eventswarm");
        instance.setup();
        assertNotNull(instance.getResult());
        System.out.println("Have " + Integer.toString(instance.getResult().getTweets().size()) + " results");
    }

    /**
     * Process results
     */
    @Test
    public void process() throws Exception {
        System.out.println("Process query '#eventswarm'");
        StatusQueryChannel instance = new StatusQueryChannel(factory.getInstance(), "#eventswarm");
        instance.registerAction(this);
        instance.process();
        assertEquals(instance.getCount(), (long) events.size());
    }

    public void execute(AddEventTrigger trigger, Event event) {
        System.out.println("Have received an event:" + event.toString());
        synchronized (events) {
            events.add(event);
            events.notify();
        }
    }

}
