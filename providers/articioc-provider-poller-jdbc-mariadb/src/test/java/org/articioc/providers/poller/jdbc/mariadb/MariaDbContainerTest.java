package org.articioc.providers.poller.jdbc.mariadb;

import org.testcontainers.containers.MariaDBContainer;

public abstract class MariaDbContainerTest {

  public static final MariaDBContainer POSTGRES_SQL_CONTAINER;

  static {
    POSTGRES_SQL_CONTAINER = new MariaDBContainer("mariadb:12-ubi");
    POSTGRES_SQL_CONTAINER.start();
  }
}
