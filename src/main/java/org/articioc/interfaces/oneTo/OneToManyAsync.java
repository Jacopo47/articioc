package org.articioc.interfaces.oneTo;

import io.vavr.collection.Stream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface OneToManyAsync<A> extends Function<A, CompletableFuture<Stream<A>>> {
  CompletableFuture<Stream<A>> apply(A input);
}
