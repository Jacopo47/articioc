package org.articioc.provider.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisStreamAsyncCommands;
import io.vavr.collection.Stream;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.articioc.base.Leaf;
import org.articioc.base.LeafCarrier;
import org.articioc.base.interfaces.CommitOperation;
import org.articioc.base.interfaces.Provider;
import org.articioc.base.utils.Futures;
import org.articioc.provider.redis.serialization.interfaces.MapToJsonEntryInStream;
import org.articioc.provider.redis.serialization.interfaces.MapToStreamEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RedisStreamProvider<A extends Leaf<M>, M> implements Provider<A> {
    private static final Logger logger = LoggerFactory.getLogger(RedisStreamProvider.class);

  private static final Duration DEFAULT_READ_BLOCK_DURATION = Duration.ofSeconds(10);
  private static final Duration DEFAULT_READ_CLAIM_DURATION = Duration.ofMinutes(5);
  private static final long DEFAULT_SIZE_FOR_READ_RESULT = 50L;

  private final RedisClient client;
  private final String key;
  private final String consumerGroup;
  private final String consumerId;

  private final Consumer<String> consumer;
  private final XReadArgs.StreamOffset<String> offset;
  private final StatefulRedisConnection<String, String> connection;
  private final RedisStreamAsyncCommands<String, String> async;
  private final XReadArgs consumerReadArguments;
  private final Function<A, XAddArgs> evaluateAddArguments;

  private final Class<A> typeOfA;
  private final MapToStreamEntry<A> mapper;

  private RedisStreamProvider(Builder<A, M> builder) {
    this.client = Objects.requireNonNull(builder.client);
    this.connection = this.client.connect();
    this.async = this.connection.async();

    this.key = Objects.requireNonNull(builder.key);
    this.consumerGroup = Objects.requireNonNull(builder.consumerGroup);
    this.consumerId = Optional.ofNullable(builder.consumerId)
        .orElseGet(() -> this.getClass().getSimpleName() + "-" + UUID.randomUUID());

    this.consumer = Consumer.from(this.consumerGroup, this.consumerId);
    this.offset = Optional.ofNullable(builder.offset)
        .orElseGet(() -> XReadArgs.StreamOffset.lastConsumed(key));
    var readCount = Optional.ofNullable(builder.count)
        .orElse(DEFAULT_SIZE_FOR_READ_RESULT);
    this.consumerReadArguments = Optional.ofNullable(builder.consumerReadArguments)
        .orElseGet(() -> XReadArgs.Builder.block(DEFAULT_READ_BLOCK_DURATION)
            .claim(DEFAULT_READ_CLAIM_DURATION))
        .count(readCount);

    this.evaluateAddArguments = Optional.ofNullable(builder.dynamicAddArguments)
        .orElseGet(() -> ignore -> Optional.ofNullable(builder.staticAddArguments)
            .orElseGet(XAddArgs::new));

    this.typeOfA = builder.typeOfA;
    this.mapper = Optional.ofNullable(builder.mapper)
        .orElseGet(() -> new MapToJsonEntryInStream<>(this.typeOfA, null));

  }

  @Override
  public CompletableFuture<Stream<LeafCarrier<A>>> read() {
    Function<StreamMessage<String, String>, Optional<LeafCarrier<A>>> toRecord = message -> {
      if (message == null) return Optional.empty();

      var leaf = Optional.of(message)
          .map(StreamMessage::getBody)
          .flatMap(mapper::deserialize)
          .orElse(null);

      if (leaf == null) return Optional.empty();

      CommitOperation<A> commit = e -> async
          .xack(key, consumerGroup, message.getId())
          .toCompletableFuture()
          .thenApply(ignore -> leaf);

      return Optional.of(leaf)
          .map(LeafCarrier::from)
          /* In redis there isn't a way to "free" a message.
          * Someone else must claim it. That's why rollback is not implemented. */
          .map(e -> e.withCommit(commit));

    };

    Function<List<StreamMessage<String, String>>, Stream<LeafCarrier<A>>> mapToLeaves = records ->
        Stream.ofAll(records)
        .map(toRecord)
        .flatMap(e -> Stream.ofAll(e.stream()));


    return async
        .xreadgroup(consumer, consumerReadArguments, offset)
        .toCompletableFuture()
        .thenApply(mapToLeaves);
  }

  @Override
  public CompletableFuture<A> write(A leaf) {
    return Optional.ofNullable(leaf)
        .flatMap(mapper::serialize)
        .map(e -> async.xadd(key, evaluateAddArguments.apply(leaf), e))
        .map(CompletionStage::toCompletableFuture)
        .orElseGet(() -> CompletableFuture.failedFuture(new Exception("Unable to write due to null input")))
        .thenApply(ignore -> leaf);
  }

  @Override
  public CompletableFuture<Stream<A>> write(Stream<A> leaves) {
    return Futures.whenAllAsStream(leaves.map(this::write));
  }

  @Override
  public void close() {
    this.connection.close();
    this.client.shutdown();
  }

  public static class Builder<A extends Leaf<M>, M> {
    private final Class<A> typeOfA;
    private final RedisClient client;
    private final String key;
    private final String consumerGroup;

    private String consumerId;
    private XReadArgs.StreamOffset<String> offset;
    private Long count;
    private XReadArgs consumerReadArguments;
    private MapToStreamEntry<A> mapper;
    private XAddArgs staticAddArguments;
    private Function<A, XAddArgs> dynamicAddArguments;

    public Builder(Class<A> typeOfA, RedisClient client, String key, String consumerGroup) {
      this.typeOfA = typeOfA;
      this.client = client;
      this.key = key;
      this.consumerGroup = consumerGroup;
    }

    public Builder<A, M> consumerId(String consumerId) {
      this.consumerId = consumerId;
      return this;
    }

    public Builder<A, M> offset(XReadArgs.StreamOffset<String> offset) {
      this.offset = offset;
      return this;
    }

    public Builder<A, M> count(Long count) {
      this.count = count;
      return this;
    }

    public Builder<A, M> consumerReadArguments(XReadArgs args) {
      this.consumerReadArguments = args;
      return this;
    }

    public Builder<A, M> mapper(MapToStreamEntry<A> mapper) {
      this.mapper = mapper;
      return this;
    }

    public Builder<A, M> addArgument(XAddArgs args) {
      this.staticAddArguments = args;
      return this;
    }

    public Builder<A, M> addArgument(Function<A, XAddArgs> evaluateArgs) {
      this.dynamicAddArguments = evaluateArgs;
      return this;
    }

    public RedisStreamProvider<A, M> build() {
      return new RedisStreamProvider<>(this);
    }
  }
}
