package com.eventswarm.social.channels;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public interface PubSubContentHandler {
    /**
     * Implementations must provide this method for processing content in the request
     * body with the specified headers.
     *
     * Implementations must internalise exceptions and should attempt to handle the content as quickly
     * as possible, since the PubSubHubbub protocol handler waits for this method to complete before responding.
     *
     * Note that this interface assumes the body will be string encoded. Might need to revisit if we have any
     * issues here.
     *
     * @param body InputStream for reading the HTTP request body
     * @param headers Map of headers
     */
    public void handle(InputStream body, Map<String, List<String>> headers);
}
