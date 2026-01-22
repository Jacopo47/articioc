package org.articioc.provider.kafka;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import org.articioc.base.interfaces.Provider;
import org.articioc.provider.kafka.containers.KafkaContainerTest;
import org.articioc.tests.models.TestStep;
import org.articioc.tests.scenarios.BasicProviderTestLeaf;
import org.articioc.tests.scenarios.ProviderBasicBehavioursIntegrationTest;

class KafkaProviderTest extends KafkaContainerTest
    implements ProviderBasicBehavioursIntegrationTest<KafkaProviderTest.TestLeaf, TestStep> {

  public static class TestLeaf extends BasicProviderTestLeaf<TestStep> {

    @Override
    public String key() {
      return "";
    }
  }

  private final Provider<TestLeaf> provider;

  public KafkaProviderTest() throws ClassNotFoundException, UnknownHostException {
    var consumerConf = new Properties();
    consumerConf.put("bootstrap.servers", KAFKA_CONTAINER.getBootstrapServers());
    consumerConf.put("group.id", "test-group");
    consumerConf.put("client.id", InetAddress.getLocalHost().getHostName());

    var producerConf = new Properties();
    producerConf.put("bootstrap.servers", KAFKA_CONTAINER.getBootstrapServers());
    producerConf.put("client.id", InetAddress.getLocalHost().getHostName());

    this.provider = new KafkaProvider<>(TestLeaf.class, "test-topic", consumerConf, producerConf);
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
