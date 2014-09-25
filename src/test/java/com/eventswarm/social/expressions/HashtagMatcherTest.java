package com.eventswarm.social.expressions;

import com.eventswarm.social.events.TweetEvent;
import com.eventswarm.social.helpers.StatusReader;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 3/10/13
 * Time: 5:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class HashtagMatcherTest {
    @Test
    public void testMatchesLowerCaseNoHash() throws Exception {
        HashtagMatcher matcher = new HashtagMatcher("news");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertTrue(matcher.matches(event));
    }

    @Test
    public void testMatchesLowerCaseWithHash() throws Exception {
        HashtagMatcher matcher = new HashtagMatcher("#news");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertTrue(matcher.matches(event));
    }

    @Test
    public void testMatchesMixedCase() throws Exception {
        HashtagMatcher matcher = new HashtagMatcher("News");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertTrue(matcher.matches(event));
    }

    @Test
    public void testMatchesAllTags() throws Exception {
        HashtagMatcher matcher1 = new HashtagMatcher("news");
        HashtagMatcher matcher2 = new HashtagMatcher("topstories");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertTrue(matcher1.matches(event));
        assertTrue(matcher2.matches(event));
    }

    @Test
    public void testNotMatches() throws Exception {
        HashtagMatcher matcher = new HashtagMatcher("newsy");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertFalse(matcher.matches(event));
    }

    @Test
    public void testCashtagMatches() throws Exception {
        HashtagMatcher matcher = new HashtagMatcher("obr");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single1hash1cash.json"));
        assertTrue(matcher.matches(event));
    }
}
