package org.articioc.providers.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.articioc.providers.jdbc.mappers.JdbcPollerRowMapper;
import org.articioc.providers.jdbc.models.JdbcPollerTestLeaf;
import org.articioc.providers.poller.PollerRecordStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JdbcPollerRowMapperFactoryTest {

  JdbcPollerRowMapper<JdbcPollerTestLeaf> mapper;

  public JdbcPollerRowMapperFactoryTest() {
    this.mapper = new JdbcPollerRowMapper<>(JdbcPollerTestLeaf.class, JdbcPollerTestLeaf.creator());
  }

  @Test
  void basicScenario() throws SQLException {
    UUID id = UUID.randomUUID();

    try (ResultSet resultSet = mock(ResultSet.class)) {
      when(resultSet.next()).thenReturn(true).thenReturn(false);
      when(resultSet.getString("id")).thenReturn(id.toString());
      when(resultSet.getString("step")).thenReturn("FIRST");
      when(resultSet.getInt("status")).thenReturn(0);
      when(resultSet.getInt("tentative")).thenReturn(1);
      when(resultSet.getString("payload")).thenReturn(null);

      var res = mapper.map(resultSet, null);

      assertEquals(id, res.getId());
      assertEquals("FIRST", res.getStep().getName());
      assertEquals(PollerRecordStatus.Ready, res.getMetadata().getStatus());
      assertEquals(1, res.getMetadata().getTentative());
      assertEquals(Optional.empty(), res.getPayload());
    }
  }

  @Test
  void nullSafe() throws SQLException {
    UUID id = UUID.randomUUID();

    try (ResultSet resultSet = mock(ResultSet.class)) {
      when(resultSet.next()).thenReturn(true).thenReturn(false);
      when(resultSet.getString("id")).thenReturn(null);
      when(resultSet.getString("step")).thenReturn(null);
      when(resultSet.getInt("status")).thenReturn(0);
      when(resultSet.getInt("tentative")).thenReturn(0);
      when(resultSet.getString("payload")).thenReturn(null);

      Assertions.assertThrows(NoSuchElementException.class, () -> mapper.map(resultSet, null));

      when(resultSet.getString("id")).thenReturn(id.toString());

      var res = mapper.map(resultSet, null);

      assertEquals(id, res.getId());
      assertNull(res.getStep().getName());
      assertEquals(PollerRecordStatus.Ready, res.getMetadata().getStatus());
      assertEquals(0, res.getMetadata().getTentative());
      assertEquals(Optional.empty(), res.getPayload());
    }
  }
}
