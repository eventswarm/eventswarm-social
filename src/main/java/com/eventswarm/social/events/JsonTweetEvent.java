package com.eventswarm.social.events;

import com.eventswarm.events.*;
import com.eventswarm.events.jdo.JdoHeader;
import com.eventswarm.events.jdo.OrgJsonEvent;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import twitter4j.Status;
import twitter4j.TwitterObjectFactory;

import java.lang.ref.SoftReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Subclass of OrgJsonEvent created specifically for tweet events, mostly so we can set headers etc appropriately
 * from information in the tweet.
 *
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class JsonTweetEvent extends OrgJsonEvent implements Keywords, OrderedKeywords, TweetEntities {
    private static String STATUS_TEXT_KEY = "text";
    public static SimpleDateFormat TWITTER_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
    private static Logger logger = Logger.getLogger(JsonTweetEvent.class);

    static {
        TWITTER_DATE_FORMAT.setLenient(true);
    }

    /**
     * Maintain a set of keywords extracted from the tweet.
     *
     * Use a soft reference to the keywords so we can avoid holding on to them when we run low on memory, but
     * also allowing them to be held onto for efficiency because we typically use them more than once.
     */
    private SoftReference<Set<String>> keywords;
    private SoftReference<List<String>> orderedKeywords;

    /**
     * Create a new JsonTweetEvent from a twitter4j status object
     *
     * This constructor will only work if the twitter4j configuration has twitter4j.jsonStoreEnabled set to true
     * and the constructor is called from the thread that received the status object.
     */
    public JsonTweetEvent(Status tweet) {
        construct(makeHeader(tweet),
                new JSONObject(getJSONString(tweet)));
        this.extractKeywords();
    }

    /**
     * Create a new JsonTweetEvent from raw JSON source for the tweet
     *
     * @return
     */
    public JsonTweetEvent(String tweet) {
        JSONObject json = new JSONObject(tweet);
        Header header = new JdoHeader(parseTweetDate(json.getString("created_at")), TweetEvent.EVENT_SOURCE, tweetUrlFor(json));
        construct(header, json);
    }

    /**
     * Create a new JsonTweetEvent from a header and raw JSON source for the tweet
     *
     * This constructor is primarily intended for reconstructing persisted events, since we want to reconstruct
     * the header exactly.
     */
    public JsonTweetEvent(Header header, String tweet) {
        construct(header, new JSONObject(tweet));
    }

    /**
     * Create a new JsonTweetEvent from a header and a JSON object parsed from a tweet or other stored source
     *
     * @param header
     * @param json
     */
    public JsonTweetEvent(Header header, JSONObject json) {
        construct(header, json);
    }

    protected void construct(Header header, JSONObject json) {
        super.construct(header, json);
        this.extractKeywords();
    }


   /**
     * Parse a twitter date string, returning the date if successful, or current date/time if not
     *
     * We catch and log the exception because we don't want event construction to fail
     *
     * @param tweetDate
     * @return
     */
    public static Date parseTweetDate(String tweetDate) {
        try {
            return TWITTER_DATE_FORMAT.parse(tweetDate);
        } catch (ParseException exc) {
            logger.error("Error parsing tweet date:" + exc.toString());
            return new Date();
        }
    }

    /**
     * Construct a URL for a tweet given an JSONObject (JSONObject child class)
     *
     * @param json
     * @return
     */
    public static String tweetUrlFor(JSONObject json) {
        return "http://" + TweetEvent.SOURCE + "/" + json.getJSONObject("user").getString("screen_name") + "/status/" + Long.toString(json.getLong("id"));
    }

    public Long getTweetId() {
        return this.json.getLong("id");
    }

    public String getTweetUrl() {
        return tweetUrlFor(this.json);
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

    public String getStatusText() {
        return this.getString(STATUS_TEXT_KEY);
    }

    public Set<String> getHashtags() {
        HashSet<String> result = new HashSet<String>();
        JSONArray array = getJSONObject("entities").getJSONArray("hashtags");
        for (int i=0; i < array.length(); i++) {
            result.add(array.getJSONObject(i).getString("text").toLowerCase());
        }
        return result;
    }

    public Set<String> getCashtags() {
        HashSet<String> result = new HashSet<String>();
        JSONArray array = getJSONObject("entities").getJSONArray("hashtags");
        for (int i=0; i < array.length(); i++) {
            int start = array.getJSONObject(i).getJSONArray("indices").getInt(0);
            if (getString("text").charAt(start) == '$') {
                result.add(array.getJSONObject(i).getString("text").toLowerCase());
            }
        }
        return result;
    }

    public String getAuthor() {
        return getJSONObject("user").getString("screen_name").toLowerCase();
    }

    public Set<String> getMentions() {
        HashSet<String> result = new HashSet<String>();
        JSONArray array = getJSONObject("entities").getJSONArray("user_mentions");
        for (int i=0; i < array.length(); i++) {
            result.add(array.getJSONObject(i).getString("screen_name").toLowerCase());
        }
        return result;
    }

    public boolean isRetweet() {
        return getBoolean("retweeted");  //To change body of implemented methods use File | Settings | File Templates.
    }

    private void extractKeywords() {
        List<String> words = TweetEvent.getKeywords(this.getStatusText());
        this.keywords = new SoftReference<Set<String>>(new HashSet<String>(words));
        this.orderedKeywords = new SoftReference<List<String>>(words);
    }

    private static String getJSONString(Status tweet) {
        String result = TwitterObjectFactory.getRawJSON(tweet);
        if (result == null) {
            System.out.println("Raw JSON not available in tweet");
            logger.error("Raw JSON not available in tweet");
        }
        return result;
    }

    private static Header makeHeader(Status tweet) {
        return new JdoHeader(tweet.getCreatedAt(),
                             TweetEvent.EVENT_SOURCE,
                             TweetEvent.tweetUrlFor(tweet).toString());
    }
}
