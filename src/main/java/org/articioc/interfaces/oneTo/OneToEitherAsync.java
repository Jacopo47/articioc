package org.articioc.interfaces.oneTo;

import io.vavr.control.Either;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface OneToEitherAsync<E extends Exception, A>
    extends Function<A, CompletableFuture<Either<E, A>>> {
  CompletableFuture<Either<E, A>> apply(A input);
}
