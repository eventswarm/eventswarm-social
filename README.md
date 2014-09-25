# EventSwarm Social

EventSwarm social is a library of components for connecting EventSwarm
to social media sources. Right now, it’s just twitter (either
synchronous queries or the public stream), but we’ll be extending it
over time.

## Install

EventSwarm social is mavenized, so it is best to add it as a project
maven dependency. Otherwise, build or download the jar and add the
twitter4j dependencies (`twitter4j-core` and `twitter4j-stream`) from
http://twitter4j.org. The library also depends on log4j 1.2.x, org.json
and eventswarm.

## Credentials

EventSwarm social needs twitter application credentials to connect to the
twitter public feed and to execute queries. If you don't already have
application credentials, add a new app to your twitter account and create
the necessary credentials. Then configure credentials in a a
`twitter4j.properties` file in your classpath. A template with placeholders
is provided in `src/main/resources/templates`. For developers, we
recommend that you symbolically link your `twitter4j.properties` into
`src/main/resources`. This ensures you don’t accidentally push your
credentials into the git repository.

Alternatively, you can use the twitter4j `ConfigurationBuilder` class
and set the credentials programmatically. See the
`JsonStatusQueryChannelTest` unit test class for an example (it reads
them from the properties file).

## Use

### Monitor the public stream:

1.  Create a `JsonStatusListenerChannel` instance, preferably passing a
    `TwitterStreamFactory` instance (better to share a single instance across
    channels if you use more than one).

2.  Set track keywords (i.e. query strings) and followed IDs using the
    `resetFilter` method. If you have screen names, you’ll need to
    convert them to IDs using the twitter4j API.

3.  Connect your processing graph to the channel using the
    `registerAction` method.

4.  Connect to twitter by calling the `connect` method.

### Process queries:

1.  Create a `JsonStatusQueryChannel` instance, passing a query string
    and a `TwitterFactory` instance or `Twitter` instance.

2.  If you want to limit the scope of your query to return only new
    tweets, use the `setSince` method, passing the most recently
    received tweet.

3.  Connect your processing graph to the channel using the
    `registerAction` method.

4.  Call the `process` method to execute the query and process the
    result.

### Notes

-   we have two streaming channels, the `JsonStatusListenerChannel`
    which uses org.json.JSONObject instances, and the
    `StatusListenerChannel` which uses the native twitter4j `Status`
    instances. We’ve found it much easier to work with the JSON
    representation and our `JsonEvent` class, especially when working
    with multiple data sources. We still use twitter4j for the protocol
    handling, however: it does that very well. We might deprecate the
    use of twitter4j `Status` instances in future.

-   if you want to be independent of the underlying in-memory format,
    use the `TweetEntities` interface rather than a specific event
    class.

-   We have retrievers for tweet author, mentions and hashtags on a
    `TweetEntities` instance. These can be used with the various
    expression and matcher classes in EventSwarm.

-   The event classes implement both the Keyword and Keywords
    interfaces, which can be used for word extraction and matching.


