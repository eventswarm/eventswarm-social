package com.eventswarm.social.events;

import com.eventswarm.abstractions.ValueRetriever;
import com.eventswarm.events.Event;

import java.util.List;

/**
 * Interface for event classes that encapsulate an HTML fragment
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public interface HtmlFragmentEvent {
    public static final String FRAGMENT_PART = "HTML_FRAGMENT";

    /**
     * @return first subordinate HTML element matching a CSS selector as a string
     */
    public String getHtml(String selector);

    /**
     * @return all subordinate HTML elements matching a CSS selector as a string
     */
    public List<String> getAllHtml(String selector);

    /**
     * @return text content of first HTML element matching a CSS selector, excluding children
     */
    public String getOwnText(String selector);

    /**
     * @return text content of all HTML elements matching a CSS selector as a string, excluding children
     */
    public List<String> getAllOwnText(String selector);

    /**
     * @return text content of first HTML element matching a CSS selector, including all child elements
     */
    public String getText(String selector);

    /**
     * @return text content of all HTML elements matching a CSS selector, including all child elements
     */
    public List<String> getAllText(String selector);

    /**
     * @return HTML element name
     */

    /**
     * Retriever implementation for getText() method
     */
    public static class TextRetriever implements ValueRetriever<String> {
        private String selector;

        public TextRetriever(String selector) {
            this.selector = selector;
        }

        public String getValue(Event event) {
            if (HtmlFragmentEvent.class.isInstance(event)) {
                return ((HtmlFragmentEvent) event).getText(selector);
            } else {
                return null;
            }
        }
    }

    /**
     * Retriever implementation for getOwnText() method
     */
    public static class OwnTextRetriever implements ValueRetriever<String> {
        private String selector;

        public OwnTextRetriever(String selector) {
            this.selector = selector;
        }

        public String getValue(Event event) {
            if (HtmlFragmentEvent.class.isInstance(event)) {
                return ((HtmlFragmentEvent) event).getOwnText(selector);
            } else {
                return null;
            }
        }
    }
}
