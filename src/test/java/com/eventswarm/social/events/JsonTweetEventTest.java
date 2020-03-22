package com.eventswarm.social.events;

import com.eventswarm.social.helpers.JsonEventReader;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class JsonTweetEventTest {
    List<JsonTweetEvent> events = new ArrayList<JsonTweetEvent>();

    @Test
    public void testCreate() throws Exception {
        JsonTweetEvent instance = (new JsonEventReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertNotNull(instance);
    }

    @Test
    public void testGetKeywords() throws Exception {
        JsonTweetEvent instance = (new JsonEventReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        String[] expected = {"to", "wasn", "via", "topstories", "news", "n", "meant", "the", "cnn", "i", "co", "t", "see", "http", "ib2bmuu1ef", "korea"};
        Set<String> keywords = instance.getKeywords();
        for (String word : expected) { assertTrue(keywords.contains(word)); }
    }

    @Test
    public void testGetOrderedKeywords() throws Exception {
        JsonTweetEvent instance = (new JsonEventReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        String[] expected = {"news", "topstories", "the", "n", "korea", "i", "wasn", "t", "meant", "to", "see", "http", "t", "co", "ib2bmuu1ef", "via", "cnn"};
        List<String> keywords = instance.getOrderedKeywords();
        for (int i=0; i<expected.length; i++) {
            assertEquals(expected[i], keywords.get(i));
        }
    }

    @Test
    public void testGetTweetId() throws Exception {
        JsonTweetEvent instance = (new JsonEventReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertEquals(384879482202370048L, instance.getTweetId().longValue());
    }

    @Test
    public void testGetTweetUrl() throws Exception {
        JsonTweetEvent instance = (new JsonEventReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertEquals("http://twitter.com/iRSSNews/status/384879482202370048", instance.getTweetUrl());
    }

    @Test
    public void testGetHashtags() throws Exception {
        JsonTweetEvent instance = (new JsonEventReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertEquals(2, instance.getHashtags().size());
        assertTrue(instance.getHashtags().contains("news"));
        assertTrue(instance.getHashtags().contains("topstories"));
    }

    @Test
    public void testGetHashtags_with_cash() throws Exception {
        JsonTweetEvent instance = (new JsonEventReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single1hash1cash.json"));
        assertEquals(2, instance.getHashtags().size());
        assertTrue(instance.getHashtags().contains("news"));
        assertTrue(instance.getHashtags().contains("obr"));
    }

    @Test
    public void testGetCashtags_empty() throws Exception {
        JsonTweetEvent instance = (new JsonEventReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertEquals(0, instance.getCashtags().size());
    }

    @Test
    public void testGetCashtags_single() throws Exception {
        JsonTweetEvent instance = (new JsonEventReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single1hash1cash.json"));
        assertEquals(1, instance.getCashtags().size());
        assertTrue(instance.getCashtags().contains("obr"));
    }

    @Test
    public void testGetAuthor() throws Exception {
        JsonTweetEvent instance = (new JsonEventReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertEquals("irssnews", instance.getAuthor());
    }

    @Test
    public void testGetMentions() throws Exception {
        JsonTweetEvent instance = (new JsonEventReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertEquals(1, instance.getMentions().size());
        assertTrue(instance.getMentions().contains("cnn"));
    }

    @Test
    public void testIsRetweet_false() throws Exception {
        JsonTweetEvent instance = (new JsonEventReader()).getFirstEvent(this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json"));
        assertFalse(instance.isRetweet());
    }

    @Test
    public void testIsRetweet_true() throws Exception {
        // TODO: collect a tweet with retweet = true
    }
}
