package org.articioc.providers.jdbc.models;

import static org.articioc.providers.jdbc.JdbcPollerProvider.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vavr.collection.Stream;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.articioc.Articioc;
import org.articioc.interfaces.oneTo.OneToMany;
import org.articioc.interfaces.oneTo.OneToOne;
import org.articioc.providers.jdbc.JdbcPollerProvider;
import org.articioc.tests.models.TestStep;
import org.articioc.tests.scenarios.BasicOperationsProviderIntegrationTest;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public interface JdbcPollerIntegrationTest<B extends JdbcPollerTestLeaf>
    extends BasicOperationsProviderIntegrationTest<B, JdbcPollerRecordMetadata> {

  default List<Map<String, Object>> selectAll() {
    return connection().withHandle(h -> h.createQuery(
            "SELECT * FROM %s ORDER BY %s".formatted(tableName(), COLUMN_CREATED_AT))
        .mapToMap()
        .collectIntoList());
  }

  @Test
  default void isSettingAtDoneAfterLatestStep() {
    JdbcPollerProvider<B, JdbcPollerRecordMetadata> provider =
        (JdbcPollerProvider<B, JdbcPollerRecordMetadata>) this.provider();
    Supplier<CompletableFuture<Stream<B>>> trigger =
        () -> CompletableFuture.completedFuture(Stream.of((B) first()));

    var builder = new Articioc.Builder<>(provider, trigger, TestStep._0);

    var articioc = builder
        .addStep((OneToOne<B>) e -> (B) e.setStep0("Hello"))
        .addStep((OneToOne<B>) e -> (B) e.setStep1("world"))
        .checkpoint(TestStep._1)
        .addStep((OneToOne<B>) e -> (B) e.setStep2("!"))
        .end();

    var initRecords = articioc.trigger().join().toList();
    Assertions.assertFalse(initRecords.isEmpty());

    var resultSet = selectAll();
    assertEquals(1, resultSet.size());
    var record = resultSet.getFirst();
    assertNotNull(record.get(COLUMN_ID));
    assertEquals(0, record.get(COLUMN_STATUS));
    assertEquals(0, record.get(COLUMN_TENTATIVE));
    assertEquals("STEP 0", record.get(COLUMN_STEP));
    assertNotNull(record.get(COLUMN_PAYLOAD));
    assertNotNull(record.get(COLUMN_CREATED_AT));
    assertNotNull(record.get(COLUMN_UPDATED_AT));
    assertNull(record.get(COLUMN_LOCKED_AT)); assertNull(record.get(COLUMN_LOCKED_BY));

    UUID id = (UUID) record.get(COLUMN_ID);
    LocalDateTime updateAt = ((Timestamp) record.get(COLUMN_UPDATED_AT)).toLocalDateTime();

    var result = articioc.readThenExecute().join().toJavaList();

    assertEquals(1, result.size());
    var first = result.getFirst();
    assertEquals("Hello", first.getStep0());
    assertEquals("world", first.getStep1());
    assertNull(first.getStep2());

    resultSet = selectAll();
    assertEquals(1, resultSet.size());
    record = resultSet.getFirst();
    assertEquals(id, record.get(COLUMN_ID));
    assertEquals(0, record.get(COLUMN_STATUS));
    assertEquals(0, record.get(COLUMN_TENTATIVE));
    assertEquals("STEP 1", record.get(COLUMN_STEP));
    assertNotNull(record.get(COLUMN_PAYLOAD));
    assertNotNull(record.get(COLUMN_CREATED_AT));
    assertNull(record.get(COLUMN_LOCKED_AT)); assertNull(record.get(COLUMN_LOCKED_BY));
    assertNotEquals(
        ((Timestamp) record.get(COLUMN_UPDATED_AT)).toLocalDateTime(), updateAt);

    result = articioc.readThenExecute().join().toJavaList();

    assertEquals(1, result.size());
    first = result.getFirst();
    assertEquals("Hello", first.getStep0());
    assertEquals("world", first.getStep1());
    assertEquals("!", first.getStep2());

    resultSet = selectAll();
    assertEquals(1, resultSet.size());
    record = resultSet.getFirst();
    assertEquals(id, record.get(COLUMN_ID));
    assertEquals(2, record.get(COLUMN_STATUS));
    assertEquals(0, record.get(COLUMN_TENTATIVE));
    assertEquals("final", record.get(COLUMN_STEP));
    assertNotNull(record.get(COLUMN_PAYLOAD));
    assertNotNull(record.get(COLUMN_CREATED_AT));
    assertNull(record.get(COLUMN_LOCKED_AT)); assertNull(record.get(COLUMN_LOCKED_BY));
    assertTrue(
        ((Timestamp) record.get(COLUMN_UPDATED_AT)).toLocalDateTime().isAfter(updateAt));
  }

  @Test
  default void shouldIncrementTentativesWhileFailing() throws Exception {
    JdbcPollerProvider<B, JdbcPollerRecordMetadata> provider =
        (JdbcPollerProvider<B, JdbcPollerRecordMetadata>) this.provider();
    Supplier<CompletableFuture<Stream<B>>> trigger =
        () -> CompletableFuture.completedFuture(Stream.of((B) first()));

    var builder = new Articioc.Builder<>(provider, trigger, TestStep._0);

    var articioc = builder
        .addStep((OneToOne<B>) e -> (B) e.setStep0("Hello"))
        .addStep((OneToOne<B>) e -> (B) e.setStep1("world"))
        .checkpoint(TestStep._1)
        .addStep((OneToOne<B>) e -> {
          throw new RuntimeException("Unlucky :/");
        })
        .end();

    var initRecords = articioc.trigger().join().toList();
    Assertions.assertFalse(initRecords.isEmpty());

    var resultSet = selectAll();
    assertEquals(1, resultSet.size());
    var record = resultSet.getFirst();
    assertNotNull(record.get(COLUMN_ID));
    assertEquals(0, record.get(COLUMN_STATUS));
    assertEquals(0, record.get(COLUMN_TENTATIVE));
    assertEquals("STEP 0", record.get(COLUMN_STEP));
    assertNotNull(record.get(COLUMN_PAYLOAD));
    assertNotNull(record.get(COLUMN_CREATED_AT));
    assertNotNull(record.get(COLUMN_UPDATED_AT));
    assertNull(record.get(COLUMN_LOCKED_AT)); assertNull(record.get(COLUMN_LOCKED_BY));

    UUID id = (UUID) record.get(COLUMN_ID);
    LocalDateTime updateAt = ((Timestamp) record.get(COLUMN_UPDATED_AT)).toLocalDateTime();

    var result = articioc.readThenExecute().join().toJavaList();

    assertEquals(1, result.size());
    var first = result.getFirst();
    assertEquals("Hello", first.getStep0());
    assertEquals("world", first.getStep1());
    assertNull(first.getStep2());

    resultSet = resultSet = selectAll();
    assertEquals(1, resultSet.size());
    record = resultSet.getFirst();
    assertEquals(id, record.get(COLUMN_ID));
    assertEquals(0, record.get(COLUMN_STATUS));
    assertEquals(0, record.get(COLUMN_TENTATIVE));
    assertEquals("STEP 1", record.get(COLUMN_STEP));
    assertNotNull(record.get(COLUMN_PAYLOAD));
    assertNotNull(record.get(COLUMN_CREATED_AT));
    assertNull(record.get(COLUMN_LOCKED_AT)); assertNull(record.get(COLUMN_LOCKED_BY));
    assertNotEquals(
        ((Timestamp) record.get(COLUMN_UPDATED_AT)).toLocalDateTime(), updateAt);

    result = articioc.readThenExecute().join().toJavaList();

    assertEquals(0, result.size());

    resultSet = selectAll();
    assertEquals(1, resultSet.size());
    record = resultSet.getFirst();
    assertEquals(id, record.get(COLUMN_ID));
    assertEquals(0, record.get(COLUMN_STATUS));
    assertEquals(1, record.get(COLUMN_TENTATIVE));
    assertEquals("STEP 1", record.get(COLUMN_STEP));
    assertNotNull(record.get(COLUMN_PAYLOAD));
    assertNotNull(record.get(COLUMN_CREATED_AT));
    assertNull(record.get(COLUMN_LOCKED_AT)); assertNull(record.get(COLUMN_LOCKED_BY));
    assertTrue(
        ((Timestamp) record.get(COLUMN_UPDATED_AT)).toLocalDateTime().isAfter(updateAt));
  }

  @Test
  default void shouldCommitRecordWhenIsNotPropagatedToNextSteps() {
    JdbcPollerProvider<B, JdbcPollerRecordMetadata> provider =
        (JdbcPollerProvider<B, JdbcPollerRecordMetadata>) this.provider();
    Supplier<CompletableFuture<Stream<B>>> trigger =
        () -> CompletableFuture.completedFuture(Stream.of((B) first()));

    var builder = new Articioc.Builder<>(provider, trigger, TestStep._0);

    var articioc = builder
        .addStep((OneToOne<B>) e -> (B) e.setStep0("Hello"))
        .addStep((OneToOne<B>) e -> (B) e.setStep1("world"))
        .addStep((OneToMany<B>) e -> Stream.empty())
        .end();

    var initRecords = articioc.trigger().join().toList();
    Assertions.assertFalse(initRecords.isEmpty());

    var resultSet = selectAll();
    assertEquals(1, resultSet.size());
    var record = resultSet.getFirst();
    assertNotNull(record.get(COLUMN_ID));
    assertEquals(0, record.get(COLUMN_STATUS));
    assertEquals(0, record.get(COLUMN_TENTATIVE));
    assertEquals("STEP 0", record.get(COLUMN_STEP));
    assertNotNull(record.get(COLUMN_PAYLOAD));
    assertNotNull(record.get(COLUMN_CREATED_AT));
    assertNotNull(record.get(COLUMN_UPDATED_AT));
    assertNull(record.get(COLUMN_LOCKED_AT)); assertNull(record.get(COLUMN_LOCKED_BY));


    UUID id = (UUID) record.get(COLUMN_ID);
    LocalDateTime updateAt = ((Timestamp) record.get(COLUMN_UPDATED_AT)).toLocalDateTime();

    var result = articioc.readThenExecute().join().toJavaList();

    assertEquals(0, result.size());

    resultSet = selectAll();
    assertEquals(1, resultSet.size());
    record = resultSet.getFirst();
    assertEquals(id, record.get(COLUMN_ID));
    assertEquals(2, record.get(COLUMN_STATUS));
    assertEquals(0, record.get(COLUMN_TENTATIVE));
    assertEquals("STEP 0", record.get(COLUMN_STEP));
    assertNotNull(record.get(COLUMN_PAYLOAD));
    assertNotNull(record.get(COLUMN_CREATED_AT));
    assertNull(record.get(COLUMN_LOCKED_AT)); assertNull(record.get(COLUMN_LOCKED_BY));

    assertNotEquals(
        ((Timestamp) record.get(COLUMN_UPDATED_AT)).toLocalDateTime(), updateAt);
  }

  @Test
  default void shouldCreateAllTheNewRecordsInIntermediateStepsButNotForFinalStep() {
    JdbcPollerProvider<B, JdbcPollerRecordMetadata> provider =
        (JdbcPollerProvider<B, JdbcPollerRecordMetadata>) this.provider();
    Supplier<CompletableFuture<Stream<B>>> trigger =
        () -> CompletableFuture.completedFuture(Stream.of((B) first()));

    LocalDateTime start = LocalDateTime.now(Clock.systemUTC());

    var builder = new Articioc.Builder<>(provider, trigger, TestStep._0);

    OneToOne<B> verifyThatRecordIsLocked = leaf -> {
      var record = selectAll()
          .stream()
          .filter(e -> e.get(COLUMN_ID).equals(leaf.getId()))
          .findFirst()
          .orElseThrow();

      assertEquals(1, record.get(COLUMN_STATUS));
      assertEquals(provider.getId(), record.get(COLUMN_LOCKED_BY));

      assertTrue(((Timestamp) record.get(COLUMN_LOCKED_AT)).toLocalDateTime()
          .isAfter(start));

      return leaf;
    };

    var articioc = builder
        .addStep((OneToOne<B>) e -> (B) e.setStep0("Hello"))
        .addStep((OneToOne<B>) e -> (B) e.setStep1("world"))
        .addStep((OneToMany<B>) e -> Stream.of(e, (B) ((JdbcPollerTestLeaf) e.copy()).setId(null)))
        .checkpoint(TestStep._1)
        .addStep(verifyThatRecordIsLocked)
        .addStep((OneToMany<B>) e -> Stream.of(e, (B) ((JdbcPollerTestLeaf) e.copy()).setId(null)))
        .end();

    var initRecords = articioc.trigger().join().toList();
    Assertions.assertFalse(initRecords.isEmpty());

    var resultSet = selectAll();
    assertEquals(1, resultSet.size());
    var record = resultSet.getFirst();

    UUID id = (UUID) record.get(COLUMN_ID);
    LocalDateTime updateAt = ((Timestamp) record.get(COLUMN_UPDATED_AT)).toLocalDateTime();

    var result = articioc.readThenExecute()
        .join()
        .toJavaList();

    assertEquals(2, result.size());

    resultSet = selectAll();
    assertEquals(2, resultSet.size());
    record = resultSet.getFirst();
    assertEquals(id, record.get(COLUMN_ID));
    assertEquals(0, record.get(COLUMN_STATUS));
    assertEquals(0, record.get(COLUMN_TENTATIVE));
    assertEquals("STEP 1", record.get(COLUMN_STEP));
    assertNull(record.get(COLUMN_LOCKED_AT)); assertNull(record.get(COLUMN_LOCKED_BY));

    var second = resultSet.getLast();
    assertNotEquals(id, second.get(COLUMN_ID));
    assertEquals(0, second.get(COLUMN_STATUS));
    assertEquals(0, second.get(COLUMN_TENTATIVE));
    assertEquals("STEP 1", second.get(COLUMN_STEP));

    assertTrue(
        ((Timestamp) second.get(COLUMN_CREATED_AT)).toLocalDateTime().isAfter(updateAt));

    result = articioc.readThenExecute().join().toJavaList();

    assertEquals(4, result.size());

    resultSet = selectAll();
    assertEquals(2, resultSet.size());
    record = resultSet.getFirst();
    assertEquals(id, record.get(COLUMN_ID));
    assertEquals(2, record.get(COLUMN_STATUS));
    assertEquals(0, record.get(COLUMN_TENTATIVE));
    assertEquals("final", record.get(COLUMN_STEP));
    assertNull(record.get(COLUMN_LOCKED_AT)); assertNull(record.get(COLUMN_LOCKED_BY));


    second = resultSet.getLast();
    assertNotEquals(id, second.get(COLUMN_ID));
    assertEquals(2, second.get(COLUMN_STATUS));
    assertEquals(0, second.get(COLUMN_TENTATIVE));
    assertEquals("final", second.get(COLUMN_STEP));
  }

  @Test
  default void isRespectingNotBefore() throws InterruptedException {
    JdbcPollerProvider<B, JdbcPollerRecordMetadata> provider =
        (JdbcPollerProvider<B, JdbcPollerRecordMetadata>) this.provider();
    Supplier<CompletableFuture<Stream<B>>> trigger =
        () -> CompletableFuture.completedFuture(Stream.of((B) first()));

    var builder = new Articioc.Builder<>(provider, trigger, TestStep._0);

    var articioc = builder
        .addStep((OneToOne<B>) e -> (B) e.setStep0("Hello"))
        .addStep((OneToOne<B>) e -> (B) e.setStep1("world"))
        .addStep((OneToOne<B>) e -> {
          throw new RuntimeException("Unlucky :/");
        })
        .onError((ex, records) -> {
          var recordsWithDelay = records.map(r -> {
            r.getMetadata().withDelay(Duration.ofMinutes(5));
            return r;
          });
          return CompletableFuture.completedFuture(recordsWithDelay);
        })
        .checkpoint(TestStep._1)
        .addStep((OneToOne<B>) e -> (B) e.setStep2("!"))
        .end();

    var initRecords = articioc.trigger().join().toList();
    Assertions.assertFalse(initRecords.isEmpty());

    var resultSet = selectAll();
    assertEquals(1, resultSet.size());
    var record = resultSet.getFirst();
    assertNotNull(record.get(COLUMN_ID));
    assertEquals(0, record.get(COLUMN_STATUS));
    assertEquals(0, record.get(COLUMN_TENTATIVE));
    assertEquals("STEP 0", record.get(COLUMN_STEP));
    assertNotNull(record.get(COLUMN_PAYLOAD));
    assertNotNull(record.get(COLUMN_CREATED_AT));
    assertNotNull(record.get(COLUMN_UPDATED_AT));
    assertNull(record.get(COLUMN_NOT_BEFORE_AT));

    UUID id = (UUID) record.get(COLUMN_ID);
    LocalDateTime updateAt = ((Timestamp) record.get(COLUMN_UPDATED_AT)).toLocalDateTime();

    var result = articioc.readThenExecute().join().toJavaList();

    assertEquals(1, result.size());
    var first = result.getFirst();
    assertEquals("Hello", first.getStep0());
    assertEquals("world", first.getStep1());
    assertNull(first.getStep2());

    resultSet = selectAll();
    assertEquals(1, resultSet.size());
    record = resultSet.getFirst();
    assertEquals(id, record.get(COLUMN_ID));
    assertEquals(0, record.get(COLUMN_STATUS));
    assertEquals(0, record.get(COLUMN_TENTATIVE));
    assertEquals("STEP 1", record.get(COLUMN_STEP));
    assertNotNull(record.get(COLUMN_PAYLOAD));
    assertNotNull(record.get(COLUMN_CREATED_AT));
    assertNotNull(record.get(COLUMN_NOT_BEFORE_AT));
    assertNull(record.get(COLUMN_LOCKED_AT)); assertNull(record.get(COLUMN_LOCKED_BY));
    assertNotEquals(
        ((Timestamp) record.get(COLUMN_UPDATED_AT)).toLocalDateTime(), updateAt);

    /* It MUST not found any record since notBefore is set. */
    result = articioc.readThenExecute().join().toJavaList();
    assertEquals(0, result.size());

    /* Updating the value in order to not wait until the delay is reach. */
    connection().withHandle(h -> h.createUpdate(
            "UPDATE %s SET notBeforeAt = NULL WHERE id = :id".formatted(tableName()))
        .bind("id", id)
        .execute());

    result = articioc.readThenExecute().join().toJavaList();
    assertEquals(1, result.size());

    first = result.getFirst();
    assertEquals("Hello", first.getStep0());
    assertEquals("world", first.getStep1());
    assertEquals("!", first.getStep2());

    resultSet = selectAll();
    assertEquals(1, resultSet.size());
    record = resultSet.getFirst();
    assertEquals(id, record.get(COLUMN_ID));
    assertEquals(2, record.get(COLUMN_STATUS));
    assertEquals(0, record.get(COLUMN_TENTATIVE));
    assertEquals("final", record.get(COLUMN_STEP));
    assertNotNull(record.get(COLUMN_PAYLOAD));
    assertNotNull(record.get(COLUMN_CREATED_AT));
    assertTrue(
        ((Timestamp) record.get(COLUMN_UPDATED_AT)).toLocalDateTime().isAfter(updateAt));
  }

  Jdbi connection();

  String tableName();
}
