package com.eventswarm.social;

import com.eventswarm.abstractions.ValueRetriever;
import com.eventswarm.events.Event;

/**
 * ValueRetriever to extract the tweet author, including '@'
 *
 * This has been implemented in terms of the previously existing AuthorEventKey class.
 *
 * TODO: We should remove the EventKey classes and replace them with equivalent ValueRetriever classes in the long term.
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class AuthorRetriever implements ValueRetriever<String> {
    private static AuthorEventKey eventKey = new AuthorEventKey();

    public AuthorRetriever() {
        super();
    }

    public String getValue(Event event) {
        return eventKey.getKey(event);
    }
}
