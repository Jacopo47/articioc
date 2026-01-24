package org.articioc.provider.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.XGroupCreateArgs;
import io.lettuce.core.XReadArgs;
import org.articioc.base.interfaces.Provider;
import org.articioc.provider.redis.containers.RedisContainerTest;
import org.articioc.tests.models.TestStep;
import org.articioc.tests.scenarios.BasicProviderTestLeaf;
import org.articioc.tests.scenarios.ProviderBasicBehavioursIntegrationTest;

import java.net.UnknownHostException;
import java.util.UUID;

class RedisStreamProviderTest extends RedisContainerTest
    implements ProviderBasicBehavioursIntegrationTest<RedisStreamProviderTest.TestLeaf, TestStep> {

  public static class TestLeaf extends BasicProviderTestLeaf<TestStep> {

    @Override
    public String key() {
      return "";
    }
  }

  private final Provider<TestLeaf> provider;

  public RedisStreamProviderTest() throws ClassNotFoundException, UnknownHostException {
    var client = RedisClient.create(REDIS_CONTAINER.getRedisURI());

    var key = "key:test:"+ UUID.randomUUID();

    String consumerGroup = "test-group";
    client
        .connect()
        .sync()
        .xgroupCreate(XReadArgs.StreamOffset.from(key, "0-0"),
            consumerGroup,
            XGroupCreateArgs.Builder.mkstream());

    this.provider = new RedisStreamProvider<>(
      client,
        key,
        consumerGroup,
        null,
        null,
        TestLeaf.class,
        null,
        null
    );
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
