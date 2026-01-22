package org.articioc.interfaces.manyTo;

import io.vavr.collection.Stream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.articioc.base.Leaf;

public interface ManyToOperations<E, A extends Leaf<M>, M> {
  default E addStep(ManyToMany<A> step) {
    return this.addStepImplementation(step.andThen(CompletableFuture::completedFuture));
  }

  E addStepImplementation(Function<Stream<A>, CompletableFuture<Stream<A>>> step);
}
