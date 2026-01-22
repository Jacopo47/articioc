package org.articioc.exceptions;

import io.vavr.collection.Stream;

public class SkipNextSteps extends RuntimeException {

  private final Stream<?> records;

  public SkipNextSteps(Stream<?> records) {
    this.records = records;
  }

  public Stream<?> getRecords() {
    return records;
  }
}
