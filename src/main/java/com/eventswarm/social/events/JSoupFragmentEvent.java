package com.eventswarm.social.events;

import com.eventswarm.events.EventPart;
import com.eventswarm.events.Header;
import com.eventswarm.events.jdo.JdoEvent;
import com.eventswarm.events.jdo.JdoPartWrapper;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTML fragment event implementation based on JSoup
 *
 * Created with IntelliJ IDEA.
 * User: andyb
 * To change this template use File | Settings | File Templates.
 */
public class JSoupFragmentEvent extends JdoEvent implements HtmlFragmentEvent {
    private transient Element fragment;

    public JSoupFragmentEvent(Header header, Element fragment) {
        super(header, makeParts(fragment));
        this.fragment = fragment;
    }

    public static Map<String,EventPart> makeParts(Element fragment) {
        Map<String,EventPart> parts = new HashMap<String,EventPart>();
        parts.put(HtmlFragmentEvent.FRAGMENT_PART, new JdoPartWrapper<Element>(fragment));
        return parts;
    }

    public String getHtml(String selector) {
        return fragment.select(selector).first().outerHtml();
    }

    public List<String> getAllHtml(String selector) {
        Elements elements = fragment.select(selector);
        List<String> result = new ArrayList<String>(elements.size());
        for (Element element : elements) {
            result.add(element.outerHtml());
        }
        return result;
    }

    public String getOwnText(String selector) {
        return fragment.select(selector).first().ownText();
    }

    public List<String> getAllOwnText(String selector) {
        Elements elements = fragment.select(selector);
        List<String> result = new ArrayList<String>(elements.size());
        for (Element element : elements) {
            result.add(element.ownText());
        }
        return result;
    }

    public String getText(String selector) {
        return fragment.select(selector).first().text();
    }

    public List<String> getAllText(String selector) {
        Elements elements = fragment.select(selector);
        List<String> result = new ArrayList<String>(elements.size());
        for (Element element : elements) {
            result.add(element.text());
        }
        return result;
    }
}
