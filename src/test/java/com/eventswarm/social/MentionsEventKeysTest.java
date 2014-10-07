package com.eventswarm.social;

import com.eventswarm.social.events.TweetEvent;
import com.eventswarm.social.helpers.StatusReader;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class MentionsEventKeysTest {
    @Test
    public void testGetKeys_single() throws Exception {
        MentionsEventKeys keyExtractor = new MentionsEventKeys();
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        String[] expected = {"@cnn"};
        assertEquals(expected, keyExtractor.getKeys(event));
    }

    @Test
    public void testGetKeys_empty() throws Exception {
        MentionsEventKeys keyExtractor = new MentionsEventKeys();
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single1hash1cash.json"));
        String[] expected = {};
        assertEquals(expected, keyExtractor.getKeys(event));
    }
}
