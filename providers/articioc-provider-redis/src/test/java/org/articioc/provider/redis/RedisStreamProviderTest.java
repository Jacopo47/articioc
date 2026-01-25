package org.articioc.provider.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import org.articioc.base.interfaces.Provider;
import org.articioc.provider.redis.containers.RedisContainerTest;
import org.articioc.tests.models.TestStep;
import org.articioc.tests.scenarios.BasicProviderTestLeaf;
import org.articioc.tests.scenarios.ProviderBasicBehavioursIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

public class RedisStreamProviderTest extends RedisContainerTest
    implements ProviderBasicBehavioursIntegrationTest<RedisStreamProviderTest.TestLeaf, TestStep> {

  public static class TestLeaf extends BasicProviderTestLeaf<TestStep> {

    @Override
    public String key() {
      return "";
    }
  }

  private final Provider<TestLeaf> provider;

  private final String key = "key:test:" + UUID.randomUUID();
  private final String consumerGroup = "test-group";

  public RedisStreamProviderTest() {
    var client = RedisClient.create(REDIS_CONTAINER.getRedisURI());

    client
        .connect()
        .sync()
        .xgroupCreate(XReadArgs.StreamOffset.from(key, "0-0"),
            consumerGroup,
            XGroupCreateArgs.Builder.mkstream());

    this.provider = new RedisStreamProvider.Builder<>(
          TestLeaf.class,
          client,
          key,
          consumerGroup)
        /* Forcing small time windows in order to test that messages are actually claimed when no ack is provided. */
        .consumerReadArguments(
            XReadArgs.Builder
            .block(Duration.ofSeconds(2))
            .claim(Duration.ofSeconds(1)))
        .build();
  }

  @Override
  @Test
  public void basicScenario() throws Exception {
    ProviderBasicBehavioursIntegrationTest.super.basicScenario();

    /* Verifying that no pending messages are left... */
    try (var client = RedisClient.create(REDIS_CONTAINER.getRedisURI())) {
      var pending = client
          .connect()
          .sync()
          .xpending(key, consumerGroup);

      Assertions.assertEquals(0, pending.getCount());
    }
  }

  @Override
  public Provider<TestLeaf> provider() {
    return this.provider;
  }

  @Override
  public BasicProviderTestLeaf<TestStep> first() {
    return (BasicProviderTestLeaf<TestStep>) new TestLeaf().setStep(TestStep._0);
  }
}
