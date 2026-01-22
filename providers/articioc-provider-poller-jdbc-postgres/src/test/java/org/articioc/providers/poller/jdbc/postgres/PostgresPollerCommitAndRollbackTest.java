package org.articioc.providers.poller.jdbc.postgres;

import io.vavr.collection.Stream;
import java.util.Optional;
import java.util.UUID;
import org.articioc.base.Leaf;
import org.articioc.base.Step;
import org.articioc.base.interfaces.Provider;
import org.articioc.base.interfaces.leaf.WithId;
import org.articioc.base.models.CommitOperationOptions;
import org.articioc.base.models.RollbackOperationOptions;
import org.articioc.providers.jdbc.JdbcPollerProvider;
import org.articioc.providers.jdbc.mappers.JdbcPollerRowMapper;
import org.articioc.providers.jdbc.models.JdbcPollerRecordMetadata;
import org.articioc.providers.poller.PollerRecordStatus;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.articioc.providers.poller.jdbc.postgres.PostgresPollerProvider.CREATE_TABLE;

class PostgresPollerCommitAndRollbackTest extends PostgresContainerTest {

  static class TestLeaf extends Leaf<JdbcPollerRecordMetadata> implements WithId<UUID> {

    private UUID id;

    private String payload;

    protected TestLeaf(UUID id, JdbcPollerRecordMetadata metadata, Step step) {
      super(metadata, step);
      this.id = id;
    }

    @Override
    public String key() {
      return id.toString();
    }

    public Optional<String> getPayload() {
      return Optional.ofNullable(payload);
    }

    public TestLeaf setPayload(String payload) {
      this.payload = payload;
      return this;
    }

    @Override
    public UUID getId() {
      return id;
    }

    @Override
    public WithId<UUID> setId(UUID id) {
      this.id = id;
      return this;
    }
  }

  private final Jdbi connection;
  private final Provider<TestLeaf> provider;

  public PostgresPollerCommitAndRollbackTest() {
    var mapper =
        new JdbcPollerRowMapper<>(TestLeaf.class, (id, metadata, step) -> Optional.ofNullable(id)
            .map(UUID::fromString)
            .map(uuid -> new TestLeaf(uuid, metadata, step)));
    this.connection = Jdbi.create(
            POSTGRES_SQL_CONTAINER.getJdbcUrl(),
            POSTGRES_SQL_CONTAINER.getUsername(),
            POSTGRES_SQL_CONTAINER.getPassword())
        .registerRowMapper(TestLeaf.class, mapper);

    this.provider = new PostgresPollerProvider.Builder<>(connection, mapper).build();

    this.connection.withHandle(h -> h
        .define(JdbcPollerProvider.DEFINED_VAR_TABLE_NAME, "public.poller")
        .execute(CREATE_TABLE));
  }

  @BeforeEach
  public void beforeEach() {
    this.connection.useHandle(h -> {
      h.execute("TRUNCATE poller");

      h.execute("INSERT INTO public.poller (status, tentative, step) VALUES(0, 0, 'START')");
      h.execute("INSERT INTO public.poller (status, tentative, step) VALUES(0, 0, 'START')");
      h.execute("INSERT INTO public.poller (status, tentative, step) VALUES(0, 0, 'START')");
      h.execute("INSERT INTO public.poller (status, tentative, step) VALUES(0, 0, 'START')");
      h.execute("INSERT INTO public.poller (status, tentative, step) VALUES(0, 0, 'START')");
    });
  }

  @Test
  public void shouldTakeInChargeAllTheRecords() {
    var res = provider.read().join();

    Assertions.assertEquals(5, res.size());
  }

  @Test
  public void shouldNotMoveStatusToDoneOnCommit_whenStepIsNotFinal() {
    var first = provider.read().join().head();
    var commitOperation = first.getCommitOperation().orElseThrow();
    var res = commitOperation
        .commit(new CommitOperationOptions<>(Stream.of(first.getData()), null))
        .join();

    Assertions.assertEquals(PollerRecordStatus.Ready, res.getMetadata().getStatus());
    Assertions.assertEquals(0, res.getMetadata().getTentative());

    var dbValue = connection.withHandle(h -> h.createQuery("SELECT * FROM Poller WHERE id = :id")
        .bind("id", res.id)
        .mapTo(TestLeaf.class)
        .first());

    Assertions.assertEquals(PollerRecordStatus.Processing, dbValue.getMetadata().getStatus());
    Assertions.assertEquals(0, dbValue.getMetadata().getTentative());
  }

  @Test
  public void shouldMoveStatusToDoneOnCommit_whenStepIsFinal() {
    var first = provider.read().join().head();

    first.getData().setStep(Step.FINAL);

    var commitOperation = first.getCommitOperation().orElseThrow();
    var res = commitOperation
        .commit(new CommitOperationOptions<>(Stream.of(first.getData()), null))
        .join();

    Assertions.assertEquals(PollerRecordStatus.Done, res.getMetadata().getStatus());
    Assertions.assertEquals(0, res.getMetadata().getTentative());

    var dbValue = connection.withHandle(h -> h.createQuery("SELECT * FROM Poller WHERE id = :id")
        .bind("id", res.id)
        .mapTo(TestLeaf.class)
        .first());

    Assertions.assertEquals(PollerRecordStatus.Done, dbValue.getMetadata().getStatus());
    Assertions.assertEquals(0, dbValue.getMetadata().getTentative());
  }

  @Test
  public void shouldMoveStatusToReadyOnRollback() {
    var first = provider.read().join().head();

    var rollbackOperation = first.getRollbackOperation().orElseThrow();
    var res = rollbackOperation
        .rollback(new RollbackOperationOptions<>(new Exception("Unlucky :/"), null))
        .join();

    Assertions.assertEquals(PollerRecordStatus.Ready, res.getMetadata().getStatus());
    Assertions.assertEquals(1, res.getMetadata().getTentative());

    var dbValue = connection.withHandle(h -> h.createQuery("SELECT * FROM Poller WHERE id = :id")
        .bind("id", res.id)
        .mapTo(TestLeaf.class)
        .first());

    Assertions.assertEquals(PollerRecordStatus.Ready, dbValue.getMetadata().getStatus());
    Assertions.assertEquals(1, dbValue.getMetadata().getTentative());

    var repeat = provider.read().join();

    Assertions.assertEquals(1, repeat.size());

    rollbackOperation = repeat.head().getRollbackOperation().orElseThrow();
    var secondRes = rollbackOperation
        .rollback(new RollbackOperationOptions<>(new Throwable("Unlucky :/"), null))
        .join();

    Assertions.assertEquals(PollerRecordStatus.Ready, secondRes.getMetadata().getStatus());
    Assertions.assertEquals(2, secondRes.getMetadata().getTentative());

    dbValue = connection.withHandle(h -> h.createQuery("SELECT * FROM Poller WHERE id = :id")
        .bind("id", secondRes.id)
        .mapTo(TestLeaf.class)
        .first());

    Assertions.assertEquals(PollerRecordStatus.Ready, dbValue.getMetadata().getStatus());
    Assertions.assertEquals(2, dbValue.getMetadata().getTentative());
  }
}
