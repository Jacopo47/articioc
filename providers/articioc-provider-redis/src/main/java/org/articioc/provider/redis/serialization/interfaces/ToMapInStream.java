package org.articioc.provider.redis.serialization.interfaces;

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
