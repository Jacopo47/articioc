package org.articioc.providers.jdbc.models;

import java.util.Optional;
import java.util.UUID;
import org.articioc.base.Step;
import org.articioc.base.interfaces.leaf.WithId;
import org.articioc.providers.jdbc.interfaces.LeafWithMetadataCreator;
import org.articioc.tests.scenarios.BasicProviderTestLeaf;

public class JdbcPollerTestLeaf extends BasicProviderTestLeaf<JdbcPollerRecordMetadata>
    implements WithId<UUID> {
  public static LeafWithMetadataCreator<JdbcPollerTestLeaf> creator() {
    return (id, metadata, step) -> Optional.ofNullable(id)
        .map(UUID::fromString)
        .map(uuid -> new JdbcPollerTestLeaf(uuid, metadata, step));
  }

  private UUID id;

  private String payload;

  public JdbcPollerTestLeaf() {}

  public JdbcPollerTestLeaf(UUID id) {
    this(id, null, null);
  }

  public JdbcPollerTestLeaf(UUID id, JdbcPollerRecordMetadata metadata, Step step) {
    this.id = id;
    this.setStep(step);
    this.setMetadata(metadata);
  }

  @Override
  public String key() {
    return Optional.ofNullable(getId()).map(UUID::toString).orElse(null);
  }

  public Optional<String> getPayload() {
    return Optional.ofNullable(payload);
  }

  public JdbcPollerTestLeaf setPayload(String payload) {
    this.payload = payload;
    return this;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public JdbcPollerTestLeaf setId(UUID id) {
    this.id = id;
    return this;
  }

  @Override
  public BasicProviderTestLeaf<JdbcPollerRecordMetadata> copy() {
    return new JdbcPollerTestLeaf(id, getMetadata(), this.getStep())
        .setPayload(this.getPayload().orElse(null))
        .setStep0(this.getStep0())
        .setStep1(this.getStep1())
        .setStep2(this.getStep2())
        .setStep3(this.getStep3())
        .setStep4(this.getStep4());
  }
}
