package org.articioc.provider.kafka.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

public class JsonSerializer<T> implements Serializer<T>, Deserializer<T> {

  private final ObjectMapper objectMapper;
  private Class<T> tClass;

  public JsonSerializer() {
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    if (tClass == null) {
      this.tClass = (Class<T>) configs.get("articioc.deserializer.type");
    }
  }

  @Override
  public T deserialize(String topic, byte[] data) {
    try {
      TypeReference<T> t = new TypeReference<T>() {};
      T app = objectMapper.readValue(data, tClass);
      return app;
    } catch (IOException e) {
      throw new SerializationException("Error deserializing JSON message for topic " + topic, e);
    }
  }

  @Override
  public byte[] serialize(String topic, T data) {
    if (data == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsBytes(data);
    } catch (Exception e) {
      throw new SerializationException("Error serializing JSON message for topic " + topic, e);
    }
  }

  @Override
  public void close() {
    // No resources to close
  }
}
