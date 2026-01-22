package org.articioc.interfaces.oneTo;

import io.vavr.collection.Stream;
import io.vavr.control.Either;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface OneToEitherWithStreamAsync<E extends Exception, A>
    extends Function<A, CompletableFuture<Either<E, Stream<A>>>> {
  CompletableFuture<Either<E, Stream<A>>> apply(A input);
}
