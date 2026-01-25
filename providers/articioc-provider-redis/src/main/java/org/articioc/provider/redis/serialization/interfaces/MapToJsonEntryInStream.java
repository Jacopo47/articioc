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

public class MapToJsonEntryInStream<A> implements MapToStreamEntry<A> {
  private static final Logger logger = LoggerFactory.getLogger(MapToJsonEntryInStream.class);

  private final Class<A> typeOfA;
  private final ObjectMapper jsonMapper;

  public MapToJsonEntryInStream(Class<A> typeOfA, ObjectMapper json) {
    this.typeOfA = typeOfA;
    this.jsonMapper = Optional.ofNullable(json)
        .orElseGet(() -> new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE));
  }

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
}
