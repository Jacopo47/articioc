package org.articioc.providers.jdbc.mappers;

import static org.articioc.providers.jdbc.JdbcPollerProvider.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.articioc.base.Leaf;
import org.articioc.base.Step;
import org.articioc.base.interfaces.leaf.WithId;
import org.articioc.providers.jdbc.interfaces.LeafWithMetadataCreator;
import org.articioc.providers.jdbc.models.JdbcPollerRecordMetadata;
import org.articioc.providers.poller.PollerRecordStatus;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcPollerRowMapper<A extends Leaf<JdbcPollerRecordMetadata> & WithId<UUID>>
    implements RowMapper<A> {

  private final Class<A> typeOfA;

  private static final Logger logger = LoggerFactory.getLogger(JdbcPollerRowMapper.class);
  private final LeafWithMetadataCreator<A> factory;
  private final ObjectMapper json;

  public JdbcPollerRowMapper(
      Class<A> typeOfA, LeafWithMetadataCreator<A> factory, ObjectMapper json) {
    this.factory = factory;
    this.json = json;

    this.typeOfA = typeOfA;
  }

  public JdbcPollerRowMapper(Class<A> typeOfA, LeafWithMetadataCreator<A> factory) {
    this(
        typeOfA,
        factory,
        new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE));
  }

  @Override
  public A map(ResultSet rs, StatementContext ctx) throws SQLException {

    String id = rs.getString(COLUMN_ID);
    if (id == null) throw new NoSuchElementException();

    var tentative = rs.getInt(COLUMN_TENTATIVE);
    LocalDateTime notBefore = Optional.ofNullable(rs.getTimestamp(COLUMN_NOT_BEFORE_AT))
        .map(Timestamp::toLocalDateTime)
        .orElse(null);

    var metadata = Optional.of(rs.getInt(COLUMN_STATUS))
        .filter(i -> i > 0 || i < PollerRecordStatus.values().length)
        .map(i -> PollerRecordStatus.values()[i])
        .map(status -> new JdbcPollerRecordMetadata(status, tentative, notBefore))
        .orElse(null);

    Step step = new Step(rs.getString(COLUMN_STEP));

    Function<String, Leaf<JdbcPollerRecordMetadata>> buildFromPayload = payload -> {
      try {
        Leaf<JdbcPollerRecordMetadata> leaf = (Leaf<JdbcPollerRecordMetadata>)
            json.readValue(payload, typeOfA).setId(UUID.fromString(id));

        return leaf.setMetadata(metadata).setStep(step);
      } catch (JsonProcessingException ex) {
        logger.error("Unable to deserialize json payload into a valid object due to error.", ex);
        return null;
      }
    };

    return Optional.ofNullable(rs.getString(COLUMN_PAYLOAD))
        .map(buildFromPayload)
        .or(() -> factory.create(id, metadata, step))
        .map(e -> (A) e)
        .orElseThrow();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (JdbcPollerRowMapper) obj;
    return Objects.equals(this.factory, that.factory) && Objects.equals(this.json, that.json);
  }

  @Override
  public int hashCode() {
    return Objects.hash(factory, json);
  }

  @Override
  public String toString() {
    return "JdbcPollerRowMapper[" + "factory=" + factory + ", " + "json=" + json + ']';
  }
}
