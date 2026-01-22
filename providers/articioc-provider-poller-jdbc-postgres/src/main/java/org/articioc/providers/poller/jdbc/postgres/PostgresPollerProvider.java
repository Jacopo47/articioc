package org.articioc.providers.poller.jdbc.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.function.Function;
import org.articioc.base.Leaf;
import org.articioc.providers.jdbc.JdbcPollerProvider;
import org.articioc.providers.jdbc.models.QueryOptions;
import org.articioc.providers.poller.PollerMetadata;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;

public class PostgresPollerProvider<A extends Leaf<M>, M extends PollerMetadata>
    extends JdbcPollerProvider<A, M> {

  public static final String CREATE_TABLE = """
                CREATE TABLE IF NOT EXISTS <opts_table_name> (
                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                    status int4 NOT NULL,
                    tentative int4 DEFAULT 0 NULL,
                    step text NOT NULL,
                    payload jsonb NULL,
                    notbeforeat timestamptz NULL,
                    lockedAt timestamptz NULL,
                    lockedBy text NULL,
                    createdat timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
                    updatedat timestamptz DEFAULT CURRENT_TIMESTAMP NULL
                );
                """;

  private static final Function<QueryOptions, String> SELECT = options -> """
            SELECT * FROM <opts_table_name>
            WHERE status = 0
              AND (notBeforeAt IS NULL OR notBeforeAt \\< CURRENT_TIMESTAMP)
            ORDER BY createdAt ASC
            LIMIT :opts.limit
            FOR UPDATE SKIP LOCKED;
            """;

  private static final Function<QueryOptions, String> TAKE = opts -> """
            UPDATE <opts_table_name>
            SET status = 1, lockedAt = CURRENT_TIMESTAMP, lockedBy = :opts.providerId
            WHERE id = :id AND status = 0
            """;

  private static final Function<QueryOptions, String> UPSERT = opts -> """
            INSERT INTO <opts_table_name> (id, status, tentative, step, payload, notBeforeAt, createdAt, updatedAt)
            VALUES (COALESCE(:id, gen_random_uuid()), 0, 0, :step, :payload::jsonb, :notbeforeat, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (id)
            DO UPDATE SET
                status = :status,
                tentative = :tentative,
                step = :step,
                payload = :payload::jsonb,
                notBeforeAt = :notbeforeat,
                updatedAt = CURRENT_TIMESTAMP,
                lockedAt = null,
                lockedBy = null
            """;

  protected PostgresPollerProvider(
      Jdbi connection, RowMapper<A> mapper, String tableName, Integer limit, ObjectMapper json, String id) {
    super(connection, mapper, tableName, limit, json, SELECT, TAKE, UPSERT, id);
  }

  public static class Builder<A extends Leaf<M>, M extends PollerMetadata> {
    private final Jdbi connection;
    private final RowMapper<A> mapper;
    private String tableName;
    private Integer limit;
    private ObjectMapper json;
    private String id;

    public Builder(Jdbi connection, RowMapper<A> mapper) {
      this.connection = Objects.requireNonNull(connection);
      this.mapper = Objects.requireNonNull(mapper);
    }

    public PostgresPollerProvider.Builder<A, M> tableName(String tableName) {
      this.tableName = tableName;
      return this;
    }

    public PostgresPollerProvider.Builder<A, M> limit(Integer limit) {
      this.limit = limit;
      return this;
    }

    public PostgresPollerProvider.Builder<A, M> setJsonMapper(ObjectMapper json) {
      this.json = json;
      return this;
    }

    public PostgresPollerProvider.Builder<A, M> id(String id) {
      this.id = id;
      return this;
    }


    public PostgresPollerProvider<A, M> build() {
      return new PostgresPollerProvider<>(
          this.connection, this.mapper, this.tableName, this.limit, this.json, this.id);
    }
  }
}
