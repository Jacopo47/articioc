package org.articioc.interfaces.oneTo;

import io.vavr.collection.Stream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface OneToStreamCompletableFuture<A> extends Function<A, Stream<CompletableFuture<A>>> {
  Stream<CompletableFuture<A>> apply(A input);
}
