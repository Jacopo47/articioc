package org.articioc.provider.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisStreamAsyncCommands;
import io.vavr.collection.Stream;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.articioc.base.Leaf;
import org.articioc.base.LeafCarrier;
import org.articioc.base.interfaces.CommitOperation;
import org.articioc.base.interfaces.Provider;
import org.articioc.base.interfaces.RollbackOperation;
import org.articioc.base.utils.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.Option;

public class RedisStreamProvider<A extends Leaf<M>, M> implements Provider<A> {
  public interface MapToStreamEntry<T> {
    Optional<Map<String, String>> serialize(T input);
    Optional<T> deserialize(Map<String, String> input);
  }

  private static final Logger logger = LoggerFactory.getLogger(RedisStreamProvider.class);

  private final RedisClient client;
  private final String key;
  private final String consumerGroup;
  private final String consumerId;

  private final Consumer<String> consumer;
  private final XReadArgs.StreamOffset<String> offset;
  private final StatefulRedisConnection<String, String> connection;
  private final RedisStreamAsyncCommands<String, String> async;

  private final Class<A> typeOfA;
  private final MapToStreamEntry<A> mapper;
  private final ObjectMapper jsonMapper;

  public RedisStreamProvider(
      RedisClient client,
      String key,
      String consumerGroup,
      String consumerId,
      XReadArgs.StreamOffset<String> offset,
      Class<A> typeOfA,
      MapToStreamEntry<A> mapper,
      ObjectMapper json
  ) {
    this.client = Objects.requireNonNull(client);
    this.connection = this.client.connect();
    this.async = this.connection.async();

    this.key = Objects.requireNonNull(key);
    this.consumerGroup = Objects.requireNonNull(consumerGroup);
    this.consumerId = Optional.ofNullable(consumerId)
        .orElseGet(() -> this.getClass().getSimpleName() + "-" + UUID.randomUUID());

    this.consumer = Consumer.from(this.consumerGroup, this.consumerId);
    this.offset = Optional.ofNullable(offset)
        .orElseGet(() -> XReadArgs.StreamOffset.lastConsumed(key));

    this.typeOfA = typeOfA;
    this.jsonMapper = Optional.ofNullable(json)
        .orElseGet(() -> new ObjectMapper()
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE));

    this.mapper = Optional.ofNullable(mapper)
        .orElseGet(() -> new MapToStreamEntry<>() {
          @Override
          public Optional<Map<String, String>> serialize(A input) {
            try {
              return Optional.of(
                  Map.of("entry", jsonMapper.writeValueAsString(input)));
            } catch (JsonProcessingException ex) {
              logger.error("Unable to serialize object", ex);
              return Optional.empty();
            }
          }

          @Override
          public Optional<A> deserialize(Map<String, String> input) {
            return Optional.ofNullable(input)
                .map(e -> e.get("entry"))
                .flatMap(e -> {
                  try {
                    return Optional.of(jsonMapper.readValue(e, typeOfA));
                  } catch (JsonProcessingException ex) {
                    logger.error("Unable to deserialize object", ex);
                    return Optional.empty();
                  }
                });
          }
        });


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
        .filter(e -> !e.isClaimed())
        .map(toRecord)
        .flatMap(e -> Stream.ofAll(e.stream()));


    return async
        .xreadgroup(consumer, offset)
        .toCompletableFuture()
        .thenApply(mapToLeaves);
  }

  @Override
  public CompletableFuture<A> write(A leaf) {
    return Optional.ofNullable(leaf)
        .flatMap(mapper::serialize)
        .map(e -> async.xadd(key, e))
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
}
