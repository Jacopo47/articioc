package org.articioc.interfaces;

import io.vavr.collection.Stream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface Pipeline<A>
    extends Function<CompletableFuture<Stream<A>>, CompletableFuture<Stream<A>>> {
  static <A> Pipeline<A> identity() {
    return i -> i;
  }

  default Pipeline<A> andThen(Pipeline<A> after) {
    var composed = Function.super.andThen(after);
    return composed::apply;
  }
}
