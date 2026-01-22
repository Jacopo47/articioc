package org.articioc.interfaces.oneTo;

import io.vavr.control.Either;
import java.util.function.Function;

public interface OneToEither<E extends Exception, A> extends Function<A, Either<E, A>> {
  Either<E, A> apply(A input);
}
