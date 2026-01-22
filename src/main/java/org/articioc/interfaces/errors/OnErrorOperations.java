package org.articioc.interfaces.errors;

import io.vavr.collection.Stream;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import org.articioc.base.Leaf;
import org.articioc.base.Step;

public interface OnErrorOperations<E, A extends Leaf<M>, M> {

  default E onError(Step goToStep) {
    return this.onError(null, goToStep);
  }

  default E onError(
      BiFunction<Exception, Stream<A>, CompletableFuture<Stream<A>>> exceptionHandler) {
    return this.onError(exceptionHandler, null);
  }

  E onError(
      BiFunction<Exception, Stream<A>, CompletableFuture<Stream<A>>> exceptionHandler,
      Step goToStep);
}
