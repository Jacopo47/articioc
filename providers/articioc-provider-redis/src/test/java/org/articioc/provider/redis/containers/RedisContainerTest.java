package org.articioc.provider.redis.containers;


import com.redis.testcontainers.RedisContainer;

public abstract class RedisContainerTest {

  public static final RedisContainer REDIS_CONTAINER;

  static {
    REDIS_CONTAINER = new RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME
        .withTag("8"));
    REDIS_CONTAINER.start();
  }
}
