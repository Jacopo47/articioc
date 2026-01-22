package org.articioc.providers.poller.jdbc.mariadb;

import java.util.UUID;
import org.articioc.base.interfaces.Provider;
import org.articioc.providers.jdbc.JdbcPollerProvider;
import org.articioc.providers.jdbc.mappers.JdbcPollerRowMapper;
import org.articioc.providers.jdbc.models.JdbcPollerIntegrationTest;
import org.articioc.providers.jdbc.models.JdbcPollerRecordMetadata;
import org.articioc.providers.jdbc.models.JdbcPollerTestLeaf;
import org.articioc.providers.poller.PollerRecordStatus;
import org.articioc.tests.models.TestStep;
import org.articioc.tests.scenarios.BasicProviderTestLeaf;
import org.articioc.tests.scenarios.ProviderBasicBehavioursIntegrationTest;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;

public class MariaDbPollerProviderIntegrationTest extends MariaDbContainerTest
    implements ProviderBasicBehavioursIntegrationTest<JdbcPollerTestLeaf, JdbcPollerRecordMetadata>,
        JdbcPollerIntegrationTest<JdbcPollerTestLeaf> {

  private final Provider<JdbcPollerTestLeaf> provider;
  private final Jdbi connection;

  private static final String TABLE_NAME = "MariaDbJdbcPoller";

  public MariaDbPollerProviderIntegrationTest() {
    this.connection = Jdbi.create(
        POSTGRES_SQL_CONTAINER.getJdbcUrl(),
        POSTGRES_SQL_CONTAINER.getUsername(),
        POSTGRES_SQL_CONTAINER.getPassword());
    this.provider = new MariaDbPollerProvider.Builder<>(
            this.connection,
            new JdbcPollerRowMapper<>(JdbcPollerTestLeaf.class, JdbcPollerTestLeaf.creator()))
        .tableName(TABLE_NAME)
        .build();

    this.connection.withHandle(h -> h
        .define(JdbcPollerProvider.DEFINED_VAR_TABLE_NAME, TABLE_NAME)
        .execute(MariaDbPollerProvider.CREATE_TABLE));
  }

  @BeforeEach
  public void beforeEach() {
    this.connection.useHandle(h -> {
      h.execute("TRUNCATE  %1$s".formatted(TABLE_NAME));
    });
  }

  @Override
  public Provider<JdbcPollerTestLeaf> provider() {
    return this.provider;
  }

  @Override
  public BasicProviderTestLeaf<JdbcPollerRecordMetadata> first() {
    return new JdbcPollerTestLeaf(
        UUID.randomUUID(),
        new JdbcPollerRecordMetadata(PollerRecordStatus.Ready, 0, null),
        TestStep._0);
  }

  @Override
  public Jdbi connection() {
    return connection;
  }

  @Override
  public String tableName() {
    return TABLE_NAME;
  }
}
