package org.articioc.interfaces.oneTo;

import java.util.concurrent.CompletableFuture;

public interface OneToOneAsync<A> extends java.util.function.Function<A, CompletableFuture<A>> {
  CompletableFuture<A> apply(A input);
}
