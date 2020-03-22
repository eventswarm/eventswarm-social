/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.eventswarm.social.events;

import com.eventswarm.events.EventPart;
import com.eventswarm.events.Keywords;
import com.eventswarm.events.OrderedKeywords;
import com.eventswarm.events.Source;
import com.eventswarm.events.jdo.JdoEvent;
import com.eventswarm.events.jdo.JdoHeader;
import com.eventswarm.events.jdo.JdoSource;

import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.util.*;
import java.net.URL;

import twitter4j.HashtagEntity;
import twitter4j.Status;
import org.apache.log4j.Logger;
import twitter4j.UserMentionEntity;

/**
 *
 * @author andyb
 */
public class TweetEvent extends JdoEvent implements Keywords, OrderedKeywords, TweetEntities {

    private Status tweet;
    private Status retweetOf = null;
    private SoftReference<Set<String>> hashtags;
    private SoftReference<Set<String>> mentions;
    private SoftReference<Set<String>> cashtags;

    /**
     * Maintain a set of keywords extracted from the tweet.
     *
     * Use a soft reference to the keywords so we can avoid holding on to them when we run low on memory, but
     * also allowing them to be held onto for efficiency because we typically use them more than once.
     */
    private SoftReference<Set<String>> keywords;
    private SoftReference<List<String>> orderedKeywords;

    // default logger
    private static Logger log = Logger.getLogger(TweetEvent.class);

    public static String
            TWEET = "tweet",
            RETWEETOF = "retweetOf",
            SOURCE = "twitter.com";

    public static Source EVENT_SOURCE = new JdoSource(SOURCE);

    /**
     * Create a TweetEvent from a status tweet and extract whole words from the
     * tweet into an ordered keyword set.
     *
     * Note that ordered keyword set folds the case of keywords to lower case so
     * that keyword matches can be case insensitive.
     *
     * @param tweet
     */
    public TweetEvent(Status tweet) {
        super();
        Map<String, EventPart> parts = new HashMap<String, EventPart>();
        this.tweet = tweet;
        parts.put(TWEET, new TweetEventPart(this.tweet));
        if (tweet.isRetweet() && tweet.getRetweetedStatus() != null) {
            retweetOf = tweet.getRetweetedStatus();
            parts.put(RETWEETOF, new TweetEventPart(this.retweetOf));
        }

        this.setParts(parts);
        this.setHeader(new TweetHeader(this.tweet));
        this.extractKeywords();
    }

    public Set<String> getKeywords() {
        Set<String> words = this.keywords.get();
        if (words == null) {
            extractKeywords();
        }
        return this.keywords.get();
    }

    public List<String> getOrderedKeywords() {
        List<String> words = this.orderedKeywords.get();
        if (words == null) {
            extractKeywords();
        }
        return this.orderedKeywords.get();
    }

    public Long getTweetId() {
        return this.tweet.getId();
    }

    public String getTweetUrl() {
        return tweetUrlFor(this.tweet).toString();
    }

    public static URL tweetUrlFor(Status tweet) {
        try {
            return new URL("http", SOURCE,
                "/" + tweet.getUser().getScreenName() + "/status/" + Long.toString(tweet.getId()));
        } catch (MalformedURLException exc) {
            log.error(exc);
            return null;
        }
    }

    public Status getStatus() {
        return this.tweet;
    }

    public Status getRetweetOf() {
        return this.retweetOf;
    }

    public String getStatusText() {
        return this.tweet.getText();
    }

    private void extractKeywords() {
        List<String> words = getKeywords(this.getStatusText());
        this.keywords = new SoftReference<Set<String>>(new HashSet<String>(words));
        this.orderedKeywords = new SoftReference<List<String>>(words);
    }

    /**
     * Helper method to extract keywords from a string
     *
     * TODO: move this to a general-purpose method/class in EventSwarm
     *
     * @param text
     * @return
     */
    public static List<String> getKeywords(String text) {
        String candidates[] = text.split("\\W+"); // split on word boundaries
        List<String> words = new ArrayList<String>(candidates.length);
        char first;
        for (int i = 0; i < candidates.length; i++) {
            if (candidates[i].length() > 0) {
                first = candidates[i].charAt(0);
                if (first == '#' || first == '@') {
                    // ignore hashtags and user names
                } else {
                    log.debug("Extracted keyword: " + candidates[i] + " from text at time " + (new Date()).toString());
                    words.add(candidates[i].toLowerCase());
                }
            }
        }
        return words;
    }

    /**
     * Return array of hashtag strings without leading '#' or '$', folded to lower case
     *
     * Note that cashtags are included in this set
     *
     * @return
     */
    public Set<String> getHashtags() {
        if (hashtags == null) {
            Set<String> values = new HashSet<String>();
            for (HashtagEntity entity: getStatus().getHashtagEntities()) {
                values.add(entity.getText().toLowerCase());
            }
            hashtags = new SoftReference<Set<String>>(values);
        }
        return hashtags.get();
    }

    /**
     * Return array of cashtag strings without leading '$', folded to lower caser
     *
     * @return
     */
    public Set<String> getCashtags() {
        if (cashtags == null) {
            Set<String> values = new HashSet<String>();
            for (HashtagEntity entity: getStatus().getHashtagEntities()) {
                if (getStatus().getText().charAt(entity.getStart()) == '$') {
                    values.add(entity.getText().toLowerCase());
                }
            }
            cashtags = new SoftReference<Set<String>>(values);
        }
        return cashtags.get();
    }

    /**
     * Return author screen name, folded to lower case
     *
     * @return
     */
    public String getAuthor() {
        return getStatus().getUser().getScreenName().toLowerCase();
    }

    /**
     * Return array of screen names for mentioned users, folded to lower case
     *
     * @return
     */
    public Set<String> getMentions() {
        if (mentions == null) {
            Set<String> values = new HashSet<String>();
            for (UserMentionEntity entity: getStatus().getUserMentionEntities()) {
                values.add(entity.getScreenName().toLowerCase());
            }
            mentions = new SoftReference<Set<String>>(values);
        }
        return mentions.get();
    }

    public boolean isRetweet() {
        return getStatus().isRetweet();
    }

    /**
     * Private header class so we can access protected fields of the header
     */
    private class TweetHeader extends JdoHeader {
        protected TweetHeader(Status tweet) {
            super(tweet.getCreatedAt(), (int) (tweet.getId() - (Long.MAX_VALUE - (long) Integer.MAX_VALUE)), EVENT_SOURCE);
            this.eventId = Long.toString(tweet.getId()) + "@" + SOURCE;
        }
    }
}
