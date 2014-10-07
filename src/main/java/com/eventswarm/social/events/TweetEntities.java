package com.eventswarm.social.events;

import java.util.Set;

/**
 * Set of method definitions for extracting Tweet entities etc from a tweet.
 *
 * Defined as an interface so we use either Json and twitter4j implementations
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public interface TweetEntities {

    /**
     * Return the twitter tweet id for this tweet
     */
    public Long getTweetId();

    /**
     * Return a set of hashtag strings without the leading '#' or '$', folded to lower case
     *
     * @return
     */
    public Set<String> getHashtags();

    /**
     * Return a set of of cashtag strings without the leading '$', folded to lower case
     *
     * @return
     */
    public Set<String> getCashtags();

    /**
     * Return the author screen name, folded to lower case
     */
    public String getAuthor();

    /**
     * Return a list of screen names for mentions, folded to lower case
     *
     * @return
     */
    public Set<String> getMentions();

    /**
     * Return true if this tweet is a retweet
     */
    public boolean isRetweet();

    /**
     * Return URL string for this tweet
     */
    public String getTweetUrl();

    /**
     * Return the text of this tweet
     */
    public String getStatusText();
}
