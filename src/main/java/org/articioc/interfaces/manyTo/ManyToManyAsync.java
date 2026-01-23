package org.articioc.interfaces.manyTo;

import io.vavr.collection.Stream;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface ManyToManyAsync<A> extends Function<Stream<A>, CompletableFuture<Stream<A>>> {
  CompletableFuture<Stream<A>> apply(Stream<A> input);
}
