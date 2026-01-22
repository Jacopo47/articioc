package org.articioc.providers.jdbc.interfaces;

import java.util.Optional;
import org.articioc.base.Leaf;
import org.articioc.base.Step;
import org.articioc.providers.jdbc.models.JdbcPollerRecordMetadata;

public interface LeafWithMetadataCreator<A extends Leaf<JdbcPollerRecordMetadata>> {
  Optional<A> create(String id, JdbcPollerRecordMetadata metadata, Step step);
}
