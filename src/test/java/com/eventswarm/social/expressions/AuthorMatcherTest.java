package com.eventswarm.social.expressions;

import com.eventswarm.social.events.TweetEvent;
import com.eventswarm.social.helpers.StatusReader;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 3/10/13
 * Time: 4:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class AuthorMatcherTest {

    @Test
    public void testMatchesLowerCase() throws Exception {
        AuthorMatcher matcher = new AuthorMatcher("irssnews");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertTrue(matcher.matches(event));
    }

    @Test
    public void testMatchesMixedCase() throws Exception {
        AuthorMatcher matcher = new AuthorMatcher("iRSSNews");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertTrue(matcher.matches(event));
    }

    @Test
    public void testNotMatches() throws Exception {
        AuthorMatcher matcher = new AuthorMatcher("rssnews");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertFalse(matcher.matches(event));
    }

}
