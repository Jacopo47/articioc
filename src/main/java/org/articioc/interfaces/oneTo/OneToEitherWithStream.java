package org.articioc.interfaces.oneTo;

import io.vavr.collection.Stream;
import io.vavr.control.Either;
import java.util.function.Function;

public interface OneToEitherWithStream<E extends Exception, A>
    extends Function<A, Either<E, Stream<A>>> {
  Either<E, Stream<A>> apply(A input);
}
