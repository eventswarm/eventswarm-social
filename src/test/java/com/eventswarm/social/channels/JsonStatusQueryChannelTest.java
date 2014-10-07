package com.eventswarm.social.channels;

import com.eventswarm.AddEventAction;
import com.eventswarm.AddEventTrigger;
import com.eventswarm.events.Event;
import com.eventswarm.eventset.EventSet;
import com.eventswarm.social.events.JsonTweetEvent;
import com.eventswarm.social.events.TweetEntities;
import org.junit.Before;
import org.junit.Test;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class JsonStatusQueryChannelTest implements AddEventAction {
    final ArrayList<TweetEntities> events = new ArrayList<TweetEntities>();
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
        JsonStatusQueryChannel instance = new JsonStatusQueryChannel("#eventswarm");
        assertNotNull(instance.getQuery());
        assertNotNull(instance.getTwitter());
    }

    /**
     * Create with configuration object
     */
    @Test
    public void constructWithConf() throws Exception {
        System.out.println("Construct with supplied factory");
        JsonStatusQueryChannel instance = new JsonStatusQueryChannel(factory, "#eventswarm");
        assertNotNull(instance.getQuery());
        assertNotNull(instance.getTwitter());
    }

    /**
     * Create with already-created twitter object
     */
    @Test
    public void constructWithInstance() throws Exception {
        System.out.println("Construct with supplied instance");
        JsonStatusQueryChannel instance = new JsonStatusQueryChannel(factory.getInstance(), "#eventswarm");
        assertNotNull(instance.getQuery());
        assertNotNull(instance.getTwitter());
    }

    /**
     * Setup (run) query
     */
    @Test
    public void setup() throws Exception {
        System.out.println("Run query '#news'");
        JsonStatusQueryChannel instance = new JsonStatusQueryChannel(factory.getInstance(), "#news");
        instance.setup();
        assertNotNull(instance.getResult());
        System.out.println("Have " + Integer.toString(instance.getResult().getTweets().size()) + " results");
    }

    /**
     * Process results
     */
    @Test
    public void process() throws Exception {
        System.out.println("Process query '#news'");
        JsonStatusQueryChannel instance = new JsonStatusQueryChannel(factory.getInstance(), "#news");
        instance.registerAction(this);
        instance.process();
        assertEquals(instance.getCount(), (long) events.size());
        JsonTweetEvent sample = (JsonTweetEvent) events.get(0);
        assertNotNull(sample.getAuthor());
    }

    /**
     * Process results with since specified
     */
    @Test
    public void processSince() throws Exception {
        EventSet result1 = new EventSet();
        JsonStatusQueryChannel instance = new JsonStatusQueryChannel(factory.getInstance(), "#news");
        instance.registerAction(result1);
        System.out.println("Process query '#news' once");
        instance.process();
        System.out.println("First query returned " + Integer.toString(result1.size()) + " events");
        instance = new JsonStatusQueryChannel(factory.getInstance(), "#news");
        instance.setSince((TweetEntities) result1.last());
        EventSet result2 = new EventSet();
        instance.registerAction(result2);
        System.out.println("Process query '#news' twice");
        instance.process();
        System.out.println("Second query returned " + Integer.toString(result2.size()) + " events");
        if (result2.size() > 0) {
            for (Event event:result2) {
                assertFalse(result1.contains(event));
            }
        } else {
            // just assert true (succeed) if no new tweets are retrieved
            assertTrue(true);
        }
    }

    public void execute(AddEventTrigger trigger, Event event) {
        System.out.println("Have received an event:" + event.toString());
        synchronized (events) {
            events.add((TweetEntities) event);
            events.notify();
        }
    }

}
