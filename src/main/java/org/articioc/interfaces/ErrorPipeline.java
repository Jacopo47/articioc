package org.articioc.interfaces;

import io.vavr.collection.Stream;
import java.util.concurrent.CompletableFuture;
import org.articioc.base.Step;

public interface ErrorPipeline<A> {
  static <A> ErrorPipeline<A> identity() {
    return (ex, s, step) -> CompletableFuture.failedFuture(ex);
  }

  CompletableFuture<Stream<A>> apply(Exception e, Stream<A> s, Step step);
}
