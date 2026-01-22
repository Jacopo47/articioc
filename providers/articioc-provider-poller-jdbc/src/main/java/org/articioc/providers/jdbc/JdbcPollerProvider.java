package org.articioc.providers.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vavr.collection.Stream;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

import io.vavr.control.Try;
import org.articioc.base.Leaf;
import org.articioc.base.LeafCarrier;
import org.articioc.base.Step;
import org.articioc.base.utils.Futures;
import org.articioc.providers.jdbc.exceptions.NoAffectedRowsException;
import org.articioc.providers.jdbc.models.QueryOptions;
import org.articioc.providers.poller.Poller;
import org.articioc.providers.poller.PollerMetadata;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.Slf4JSqlLogger;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.stringtemplate4.StringTemplateEngine;

/**
 * JdbcPollerProvider defines basic functionalities in order to simplify the implementation of a library that polls constantly
 * an RDBMS looking for new records ready to be processed took in charge by the pipeline executor and then actually processed.
 * <p>
 * All the queries that are run by {@link JdbcPollerProvider} are decorated by a set of named variable that present the prefix: opts. and all the values contained by the record {@link QueryOptions}
 * (e.g. opts.limit , the query can assess it as a traditional named parameter: :opts.limit)
 * </p>
 * <p>
 * Under the hood {@link JdbcPollerProvider} leverages on {@link Jdbi} and defines ({@link Jdbi#define(String, Object)}) a set of variables that will be replaced by the query's template.
 * The list of available variable are exposed as static elements by {@link JdbcPollerProvider} with the prefix: DEFINED_VAR_* (e.g. DEFINED_VAR_TABLE_NAME)
 * </p>
 */
public abstract class JdbcPollerProvider<A extends Leaf<M>, M extends PollerMetadata>
    implements Poller<A, M> {

  public static final String COLUMN_ID = "id";
  public static final String COLUMN_STATUS = "status";
  public static final String COLUMN_STEP = "step";
  public static final String COLUMN_PAYLOAD = "payload";
  public static final String COLUMN_TENTATIVE = "tentative";
  public static final String COLUMN_CREATED_AT = "createdat";
  public static final String COLUMN_UPDATED_AT = "updatedat";
  public static final String COLUMN_NOT_BEFORE_AT = "notbeforeat";
  public static final String COLUMN_LOCKED_AT = "lockedat";
  public static final String COLUMN_LOCKED_BY = "lockedby";


  public static final String DEFINED_VAR_TABLE_NAME = "opts_table_name";

  private final String SELECT_QUERY;
  private final String TAKE_QUERY;
  private final String UPSERT_QUERY;

  private final QueryOptions options;

  private final Jdbi connection;
  private final GenericType<A> type;

  private final ObjectMapper json;

  protected JdbcPollerProvider(
      Jdbi connection,
      RowMapper<A> mapper,
      String tableName,
      Integer limit,
      ObjectMapper json,
      Function<QueryOptions, String> identifyRecordsQuery,
      Function<QueryOptions, String> takeQuery,
      Function<QueryOptions, String> upsertQuery,
      String id) {
    this.type = new GenericType<>() {};
    this.connection = connection
        .setSqlLogger(new Slf4JSqlLogger())
        .installPlugin(new SqlObjectPlugin())
        .setTemplateEngine(new StringTemplateEngine())
        .registerRowMapper(type, mapper);

    this.json = Optional.ofNullable(json).orElseGet(() -> new ObjectMapper()
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE));

    String providerId = Optional.ofNullable(id)
        .or(() -> Try
            .ofCallable(() -> InetAddress.getLocalHost().getHostName())
            .toJavaOptional())
        .orElseGet(() -> UUID.randomUUID().toString());

    this.options = new QueryOptions(
        Optional.ofNullable(tableName).orElse("Poller"),
        Optional.ofNullable(limit).orElse(10),
        providerId);

    this.SELECT_QUERY = identifyRecordsQuery.apply(options);
    this.TAKE_QUERY = takeQuery.apply(options);
    this.UPSERT_QUERY = upsertQuery.apply(options);
  }

  @Override
  public CompletableFuture<Stream<LeafCarrier<A>>> read() {
    Function<Handle, List<LeafCarrier<A>>> selectRecordsAndTakeInChargeToProcess = (transaction) -> {
      Function<LeafCarrier<A>, LeafCarrier<A>> withCommit = e -> e.withCommit(this.commitOperation(e.getData()));
      Function<LeafCarrier<A>, LeafCarrier<A>> withRollback = e -> e.withRollback(this.rollbackOperation(e.getData()));

      Function<A, Integer> executeUpdate = record ->  transaction.createUpdate(TAKE_QUERY)
          .bind(COLUMN_ID, UUID.fromString(record.key()))
          .bindMethods("opts", options)
          .define(DEFINED_VAR_TABLE_NAME, options.tableName())
          .defineNamedBindings()
          .execute();

      Predicate<Integer> continueIfHasModifiedAtLeastOneRecord = i -> i > 0;

      Function<A, List<LeafCarrier<A>>> tryUpdateAndReturnLeafCarrierIfOk = (record) -> Try.ofSupplier(() -> executeUpdate.apply(record))
          .filter(continueIfHasModifiedAtLeastOneRecord, NoAffectedRowsException::new)
          .map(ignore -> record)
          .map(LeafCarrier::from)
          .map(withCommit)
          .map(withRollback)
          .map(List::of)
          .getOrElseGet(ex -> {
            if (logger.isWarnEnabled()) {
              logger.warn(
                  "Error while evaluating record with keys: {} at steps: {}.",
                  record.key(),
                  record.getStep(),
                  ex);
            }

            /* It's ok in case the operation fails to give up just not considering the records as able to be processed by this instance. */
            return List.of();
          });

      return transaction.createQuery(SELECT_QUERY)
          .bindMethods("opts", options)
          .define(DEFINED_VAR_TABLE_NAME, options.tableName())
          .defineNamedBindings()
          .mapTo(type)
          .collect(Stream.collector())
          .flatMap(tryUpdateAndReturnLeafCarrierIfOk)
          .toJavaList();
    };

    return CompletableFuture.supplyAsync(() -> connection
        .inTransaction(selectRecordsAndTakeInChargeToProcess::apply))
        .thenApply(Stream::ofAll);
  }

  @Override
  public CompletableFuture<A> take(A leaf) {
    throw new RuntimeException("Method take not implemented since read han been override for Jdbc provider");
  }

  @Override
  public CompletableFuture<Stream<A>> identifyRecords() {
    throw new RuntimeException("Method identifyRecords not implemented since read han been override for Jdbc provider");
  }

  @Override
  public CompletableFuture<A> write(A leaf) {
    if (leaf == null) return CompletableFuture.failedFuture(new NoAffectedRowsException());

    Function<Jdbi, Integer> upsert = c -> {
      String payload;
      try {
        payload = json.writeValueAsString(leaf);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }

      int tentative = Optional.of(leaf)
          .map(Leaf::getMetadata)
          .map(PollerMetadata::getTentative)
          .orElse(0);

      var step = Optional.of(leaf).map(Leaf::getStep).map(Step::getName);
      int status = Optional.of(leaf)
          .map(Leaf::getMetadata)
          .map(PollerMetadata::getStatus)
          .map(Enum::ordinal)
          .orElse(0);

      Optional<LocalDateTime> notBeforeAt = Optional.of(leaf)
          .map(Leaf::getMetadata)
          .map(PollerMetadata::getNotBefore);

      return c.withHandle(h -> h.createUpdate(UPSERT_QUERY)
          .bind(COLUMN_ID, Optional.of(leaf).map(Leaf::key).map(UUID::fromString))
          .bind(COLUMN_TENTATIVE, tentative)
          .bind(COLUMN_STEP, step)
          .bind(COLUMN_STATUS, status)
          .bind(COLUMN_NOT_BEFORE_AT, notBeforeAt)
          .bind(COLUMN_PAYLOAD, payload)
          .bindMethods("opts", options)
          .define(DEFINED_VAR_TABLE_NAME, options.tableName())
          .defineNamedBindings()
          .execute());
    };

    return CompletableFuture.completedFuture(connection)
        .thenApply(upsert)
        .thenCompose(this::failIfNoRecordsWhereWrittenOrReturn)
        .thenApply(ignore -> leaf);
  }

  @Override
  public CompletableFuture<Stream<A>> write(Stream<A> leaves) {
    var writes = leaves.map(this::write);

    return Futures.whenAllAsStream(writes);
  }

  @Override
  public void close() {}

  private CompletableFuture<Integer> failIfNoRecordsWhereWrittenOrReturn(int modifiedRows) {
    if (modifiedRows == 0) return CompletableFuture.failedFuture(new NoAffectedRowsException());

    return CompletableFuture.completedFuture(modifiedRows);
  }

  @Override
  public String getId() {
    return options.providerId();
  }
}
