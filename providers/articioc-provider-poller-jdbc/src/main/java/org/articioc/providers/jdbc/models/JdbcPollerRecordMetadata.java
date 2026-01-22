package org.articioc.providers.jdbc.models;

import java.time.LocalDateTime;
import org.articioc.providers.poller.PollerMetadata;
import org.articioc.providers.poller.PollerRecordStatus;

public class JdbcPollerRecordMetadata implements PollerMetadata {

  private int tentative;
  private PollerRecordStatus status;
  private LocalDateTime notBefore;

  public JdbcPollerRecordMetadata() {}

  public JdbcPollerRecordMetadata(
      PollerRecordStatus status, int tentative, LocalDateTime notBefore) {
    this.status = status;
    this.tentative = tentative;
    this.notBefore = notBefore;
  }

  @Override
  public int getTentative() {
    return tentative;
  }

  @Override
  public PollerMetadata setTentative(int tentative) {
    this.tentative = tentative;
    return this;
  }

  @Override
  public PollerRecordStatus getStatus() {
    return status;
  }

  @Override
  public PollerMetadata setStatus(PollerRecordStatus status) {
    this.status = status;
    return this;
  }

  @Override
  public LocalDateTime getNotBefore() {
    return notBefore;
  }

  @Override
  public PollerMetadata setNotBefore(LocalDateTime notBefore) {
    this.notBefore = notBefore;

    return this;
  }
}
