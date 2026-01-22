package org.articioc.exceptions;

import io.vavr.collection.Stream;

public class SkipPipelineSinceThereAreNoRecordsToProcess extends RuntimeException {

  private final Stream<?> records;

  public SkipPipelineSinceThereAreNoRecordsToProcess(Stream<?> records) {
    this.records = records;
  }

  public Stream<?> getRecords() {
    return records;
  }
}
