package org.articioc.interfaces.oneTo;

import io.vavr.collection.Stream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.articioc.base.Leaf;
import org.articioc.base.utils.Futures;

public interface OneToOperations<E, A extends Leaf<M>, M> {
  default E addStep(OneToOne<A> step) {
    return this.addStepOnSingleElement(
        step.andThen(Stream::of).andThen(CompletableFuture::completedFuture));
  }

  default E addStep(OneToOneAsync<A> step) {
    return this.addStepOnSingleElement(e -> step.apply(e).thenApply(Stream::of));
  }

  default E addStep(OneToMany<A> step) {
    return this.addStepOnSingleElement(step.andThen(CompletableFuture::completedFuture));
  }

  default E addStep(OneToManyAsync<A> step) {
    return this.addStepOnSingleElement(step);
  }

  default E addStep(OneToEither<Exception, A> step) {
    return this.addStepOnSingleElement(step.andThen(e -> e.fold(ex -> Stream.<A>of(), Stream::of))
        .andThen(CompletableFuture::completedFuture));
  }

  default E addStep(OneToEitherAsync<Exception, A> step) {
    return this.addStepOnSingleElement(
        step.andThen(e -> e.thenApply(a -> a.fold(ex -> Stream.of(), Stream::of))));
  }

  default E addStep(OneToEitherWithStream<Exception, A> step) {
    return this.addStepOnSingleElement(
        step.andThen(e -> e.fold(ex -> Stream.<A>of(), Function.identity()))
            .andThen(CompletableFuture::completedFuture));
  }

  default E addStep(OneToEitherWithStreamAsync<Exception, A> step) {
    return this.addStepOnSingleElement(
        a -> step.apply(a).thenApply(e -> e.fold(ex -> Stream.of(), Function.identity())));
  }

  default E addStep(OneToStreamCompletableFuture<A> step) {
    return this.addStepOnSingleElement(a -> Futures.whenAllAsStream(step.apply(a)));
  }

  E addStepOnSingleElement(Function<A, CompletableFuture<Stream<A>>> step);
}
