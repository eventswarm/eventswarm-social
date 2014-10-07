package com.eventswarm.social.expressions;

import com.eventswarm.social.events.TweetEvent;
import com.eventswarm.social.helpers.StatusReader;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class CashtagMatcherTest {
    @Test
    public void testMatchesLowerCase() throws Exception {
        CashtagMatcher matcher = new CashtagMatcher("obr");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single1hash1cash.json"));
        assertTrue(matcher.matches(event));
    }

    @Test
    public void testMatchesMixedCase() throws Exception {
        CashtagMatcher matcher = new CashtagMatcher("OBR");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single1hash1cash.json"));
        assertTrue(matcher.matches(event));
    }

    @Test
    public void testMatchesWithCash() throws Exception {
        CashtagMatcher matcher = new CashtagMatcher("$obr");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single1hash1cash.json"));
        assertTrue(matcher.matches(event));
    }

    @Test
    public void testNotMatchesHash() throws Exception {
        CashtagMatcher matcher = new CashtagMatcher("news");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single1hash1cash.json"));
        assertFalse(matcher.matches(event));
    }

    @Test
    public void testNotMatchesHashWithHash() throws Exception {
        CashtagMatcher matcher = new CashtagMatcher("#news");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single1hash1cash.json"));
        assertFalse(matcher.matches(event));
    }

    @Test
    public void testNotMatchesEmpty() throws Exception {
        CashtagMatcher matcher = new CashtagMatcher("obr");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertFalse(matcher.matches(event));
    }
}

