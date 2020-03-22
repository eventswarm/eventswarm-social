package com.eventswarm.social.channels;

import com.eventswarm.channels.AbstractChannel;
import com.eventswarm.events.Event;
import com.eventswarm.social.events.TweetEntities;
import com.eventswarm.social.events.TweetEvent;
import org.apache.log4j.Logger;
import twitter4j.*;

import java.util.Iterator;

/**
 * Channel to encapsulate a twitter query as an stream of EventSwarm events
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 24/07/13
 * Time: 11:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class StatusQueryChannel extends AbstractChannel {
    private Query query;
    private QueryResult result;
    protected Iterator<Status> iterator;
    private Twitter twitter;
    private TweetEntities since;

    private static final String RECENT = "recent";
    private static int MAX_RESULTS = 25;
    private static Logger logger = Logger.getLogger(StatusQueryChannel.class);

    /**
     * Create a status query channel using the default twitter instance.
     *
     * @param query
     */
    public StatusQueryChannel(String query) {
        this.twitter = TwitterFactory.getSingleton();
        init(query);
    }

    /**
     * Create a status query channel using a twitter instance provided by the supplied factory.
     *
     * It is assumed this constructor would be used when twitter credentials are set programatically rather than from
     * a file.
     *
     * @param factory
     * @param query
     */
    public StatusQueryChannel(TwitterFactory factory, String query) {
        this.twitter = factory.getInstance();
        init(query);
    }

    /**
     * Create a status query channel using the supplied twitter instance (i.e. someone else is getting instances from the factory)
     *
     * @param query
     */
    public StatusQueryChannel(Twitter twitter, String query) {
        this.twitter = twitter;
        init(query);
    }

    private void init(String query) {
        this.query = new Query(query);
        this.query.resultType(Query.ResultType.recent);
        this.query.count(MAX_RESULTS);
    }

    public TweetEntities getSince() {
        return since;
    }

    /**
     * Set the query to return only tweets occurring since the specified tweet
     *
     * @param since
     * @return
     */
    public StatusQueryChannel setSince(TweetEntities since) {
        this.since = since;
        if (since != null) {
            logger.debug("Excluding tweets up to and including" + Long.toString(since.getTweetId()));
            this.query = this.query.sinceId(since.getTweetId());
        }
        return this;
    }

    @Override
    public void setup() throws Exception {
        result = twitter.search(query);
        iterator = result.getTweets().iterator();
    }

    @Override
    public void teardown() throws Exception {
        result = null;
        iterator = null;
    }

    @Override
    public Event next() throws Exception {
        if (iterator.hasNext()) {
            return new TweetEvent(iterator.next());
        } else {
            this.stop();
            return null;
        }
    }

    protected Query getQuery() {
        return query;
    }

    protected QueryResult getResult() {
        return result;
    }

    protected Twitter getTwitter() {
        return twitter;
    }
}
