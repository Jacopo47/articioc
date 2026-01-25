# Articioc Provider for Redis Streams

This module provides a `Provider` implementation on top of Redis Streams.

## How it Fits with Redis Streams

The `RedisStreamProvider` maps core Articioc concepts directly to Redis Streams features.
By default, then this behavior can be driven by various configurations, `RedisStreamProvider` works out-of-the-box with:
*   **Checkpoints & Durability (`XADD`)**: When a workflow reaches a `.checkpoint()`, the `Leaf` is serialized and appended to a Redis Stream using the `XADD` command.
*   **Reliable Processing (`XREADGROUP` & `XACK`)**: Workers use `XREADGROUP` to read `Leaf` objects from the stream. When a `Leaf` and its subsequent pipeline segment are processed successfully, the provider sends an `XACK` command to Redis. This acknowledges the message and removes it from the *Pending Entries List (PEL)* for that consumer.
*   **Scalability & Workers (Consumer Groups)**: Multiple running instances of your Articioc application map directly to consumers within a single **Redis Consumer Group**. Redis handles the distribution of messages (`Leaf` objects) from the stream to the available consumers, allowing you to scale your processing power simply by adding more worker instances.
*   **Fault-Tolerance (Pending Entries & Claiming)**: If a worker crashes or fails to `XACK` a message, that message remains in the PEL. After a configurable timeout, other active consumers in the group can **claim** the pending message and re-process it. This is a core feature of Redis Streams that provides excellent fault tolerance and prevents data from being lost.

## Getting started

### Dependencies

To use this provider, add the following dependency to your build file:

```kotlin
// build.gradle.kts
implementation("org.articioc:articioc-provider-redis:x.y.z")
```

### Provider

```java
Provider<MyLeaf> provider = new RedisStreamProvider.Builder<>(
        MyLeaf.class, /* Your leaf's type. */
        redisClient,  /* An instance of io.lettuce.core.RedisClient. */
        "my-stream-key",
        "my-consumer-group" /* Consumer group MUST exist otherwise read will throw an error. */
    )
    .build();
```

## Configuration

The `RedisStreamProvider.Builder` allows you to customize its behavior:

| Method                  | Description                                                                                                                            | Default                                                                           |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| `consumerId(String)`    | A unique identifier for the consumer instance within the group.                                                                        | A random UUID.                                                                    |
| `offset(StreamOffset)`  | The stream offset to start reading from.                                                                                               | `>`                                                                               |
| `count(Long)`           | The maximum number of messages to read in a single `XREADGROUP` call. This will always win over consumerReadAerguments' value.         | `50`                                                                              |
| `consumerReadArguments(XReadArgs)` | Advanced arguments for `XREADGROUP`, like `block()` for waiting for messages or `claim()` for timeout settings.             | `block(10s).claim(5m)`                                                            |
| `mapper(MapToStreamEntry)` | A custom implementation for serializing/deserializing `Leaf` objects to/from a `Map<String, String>` for the Redis stream entry.    | A JSON-based serializer. Serializes the object into a Map("entry", "{.. json ..}")|
| `json(ObjectMapper)`    | A custom Jackson `ObjectMapper` instance to use for the default JSON serialization.                                                    | A default `ObjectMapper` with common modules.                                     |
