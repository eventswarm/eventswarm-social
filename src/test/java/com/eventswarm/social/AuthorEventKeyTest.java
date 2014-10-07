package com.eventswarm.social;

import com.eventswarm.social.events.TweetEvent;
import com.eventswarm.social.helpers.StatusReader;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class AuthorEventKeyTest {
    InputStream input;

    @Before
    public void setup() throws Exception {
        input = this.getClass().getClassLoader().getResourceAsStream("fixtures/single2hash1mention.json");
        if (input == null) {
            throw new Exception("Input is null");
        }
    }
    @Test
    public void testGetKey() throws Exception {
        AuthorEventKey keyExtractor = new AuthorEventKey();
        TweetEvent event = (new StatusReader()).getFirstEvent(input);
        assertEquals("@irssnews", keyExtractor.getKey(event));
    }
}
