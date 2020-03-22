package com.eventswarm.social;

import com.eventswarm.social.events.TweetEvent;
import com.eventswarm.social.helpers.StatusReader;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class TagEventKeysTest {
    @Test
    public void testGetKeys() throws Exception {
        TagEventKeys keyExtractor = new TagEventKeys();
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        String[] keys = {"#news", "#topstories"};
        List<String> expected = Arrays.asList(keys);
        for (String key : keyExtractor.getKeys(event)) {
            assertTrue(expected.contains(key));
        }
    }

    @Test
    public void testGetKeysWithCash() throws Exception {
        TagEventKeys keyExtractor = new TagEventKeys();
        TweetEvent event = (new StatusReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single1hash1cash.json"));
        String[] keys = {"#news", "$obr"};
        List<String> expected = Arrays.asList(keys);
        for (String key : keyExtractor.getKeys(event)) {
            assertTrue(expected.contains(key));
        }
    }
}
