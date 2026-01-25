package org.articioc.provider.redis.serialization.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class ToMapInStream<A> implements MapToStreamEntry<A> {
  private static final Logger logger = LoggerFactory.getLogger(ToMapInStream.class);

  public ToMapInStream() {
  }

  @Override
  public Optional<Map<String, String>> serialize(A input) {
    return Optional.empty();
  }

  @Override
  public Optional<A> deserialize(Map<String, String> input) {
    return Optional.empty();
  }
}
