package com.eventswarm.social;

import com.eventswarm.eventset.EventSet;
import com.eventswarm.powerset.EventSetFactory;
import com.eventswarm.powerset.Powerset;

/**
 * Created with IntelliJ IDEA.
 * User: andyb
 * Date: 4/12/12
 * Time: 9:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class TweetSetFactory implements EventSetFactory<String> {
    public EventSet createEventSet(Powerset<String> keytypePowerset, String keytype) {
        return new EventSet();
    }
}
