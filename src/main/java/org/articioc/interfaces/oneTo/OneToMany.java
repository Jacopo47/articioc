package org.articioc.interfaces.oneTo;

import io.vavr.collection.Stream;
import java.util.function.Function;

public interface OneToMany<A> extends Function<A, Stream<A>> {
  Stream<A> apply(A input);
}
