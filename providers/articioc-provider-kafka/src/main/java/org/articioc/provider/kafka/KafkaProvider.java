package org.articioc.provider.kafka;

import io.vavr.collection.Stream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.StreamSupport;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.articioc.base.Leaf;
import org.articioc.base.LeafCarrier;
import org.articioc.base.interfaces.Provider;
import org.articioc.base.utils.Futures;
import org.articioc.provider.kafka.serialization.JsonSerializer;

public class KafkaProvider<A extends Leaf<M>, M> implements Provider<A> {
  private static final Duration DEFAULT_POLL_DURATION = Duration.ofSeconds(10);

  private final String topic;
  private final KafkaConsumer<String, A> consumer;
  private final KafkaProducer<String, A> producer;
  private final Duration pollTimeout;

  public KafkaProvider(
      Class<A> type,
      String topic,
      Properties consumerProperties,
      Properties producerProperties,
      Duration pollTimeout) {
    this.topic = topic;

    consumerProperties.put(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    consumerProperties.putIfAbsent(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
    consumerProperties.putIfAbsent("articioc.deserializer.type", type);
    consumerProperties.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    this.consumer = new KafkaConsumer<>(consumerProperties);
    consumer.subscribe(List.of(this.topic));

    producerProperties.put(
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    producerProperties.put(
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
    this.producer = new KafkaProducer<>(producerProperties);

    this.pollTimeout = Optional.ofNullable(pollTimeout).orElse(DEFAULT_POLL_DURATION);
  }

  public KafkaProvider(
      Class<A> type, String topic, Properties consumerProperties, Properties producerProperties)
      throws ClassNotFoundException {
    this(type, topic, consumerProperties, producerProperties, null);
  }

  @Override
  public CompletableFuture<Stream<LeafCarrier<A>>> read() {
    ConsumerRecords<String, A> records = consumer.poll(pollTimeout);

    Stream<LeafCarrier<A>> app = Stream.ofAll(StreamSupport.stream(records.spliterator(), false))
        .map(r -> Optional.ofNullable(r)
            .map(ConsumerRecord::value)
            .map(v -> LeafCarrier.from(v)
                .withCommit((options) -> CompletableFuture.supplyAsync(() -> {
                  consumer.commitSync(Map.of(
                      new TopicPartition(r.topic(), r.partition()),
                      new OffsetAndMetadata(r.offset())));

                  return v;
                })))
            .orElseThrow());

    return CompletableFuture.completedFuture(app);
  }

  @Override
  public CompletableFuture<A> write(A leaf) {
    var record = new ProducerRecord<>(this.topic, leaf.key(), leaf);

    return CompletableFuture.supplyAsync(() -> {
          try {
            return producer.send(record).get();
          } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          }
        })
        .thenApply(ignore -> leaf);
  }

  @Override
  public CompletableFuture<Stream<A>> write(Stream<A> leaves) {
    return Futures.whenAllAsStream(leaves.map(this::write));
  }

  @Override
  public void close() {
    consumer.close();
    producer.close();
  }
}
