package org.articioc.providers.poller.jdbc.postgres;

import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class PostgresContainerTest {

  public static final PostgreSQLContainer POSTGRES_SQL_CONTAINER;

  static {
    POSTGRES_SQL_CONTAINER = new PostgreSQLContainer("postgres:18");
    POSTGRES_SQL_CONTAINER.start();
  }
}
