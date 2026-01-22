package org.articioc.interfaces.manyTo;

import io.vavr.collection.Stream;
import java.util.function.Function;

public interface ManyToMany<A> extends Function<Stream<A>, Stream<A>> {}
