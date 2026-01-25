package org.articioc.provider.redis.serialization.interfaces;

import java.util.Map;
import java.util.Optional;

public interface MapToStreamEntry<T> {
  Optional<Map<String, String>> serialize(T input);
  Optional<T> deserialize(Map<String, String> input);
}
