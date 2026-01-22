package org.articioc.interfaces.oneTo;

import java.util.function.Function;

public interface OneToOne<A> extends Function<A, A> {
  A apply(A input);
}
