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
public class MentionsMatcherTest {

    @Test
    public void testMatchesLowerCase() throws Exception {
        MentionsMatcher matcher = new MentionsMatcher("cnn");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertTrue(matcher.matches(event));
    }

    @Test
    public void testMatchesMixedCase() throws Exception {
        MentionsMatcher matcher = new MentionsMatcher("CNN");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertTrue(matcher.matches(event));
    }

    @Test
    public void testNotMatches() throws Exception {
        MentionsMatcher matcher = new MentionsMatcher("CNNN");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertFalse(matcher.matches(event));
    }

    @Test
    public void testEmptyMentions() throws Exception {
        MentionsMatcher matcher = new MentionsMatcher("CNNN");
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single1hash1cash.json"));
        assertFalse(matcher.matches(event));
    }
}
