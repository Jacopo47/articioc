package org.articioc.provider.kafka.containers;

import org.testcontainers.kafka.KafkaContainer;

public abstract class KafkaContainerTest {

  public static final KafkaContainer KAFKA_CONTAINER;

  static {
    KAFKA_CONTAINER = new KafkaContainer("apache/kafka-native:3.8.0");
    KAFKA_CONTAINER.start();
  }
}
