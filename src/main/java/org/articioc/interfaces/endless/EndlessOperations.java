package org.articioc.interfaces.endless;

import io.vavr.collection.Stream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.articioc.Articioc;
import org.articioc.StepOptions;
import org.articioc.base.Leaf;
import org.articioc.base.Step;
import org.articioc.base.interfaces.Provider;
import org.articioc.base.providers.EmptyProvider;
import org.articioc.interfaces.manyTo.ManyToOperations;

public interface EndlessOperations<E, A extends Leaf<M>, M> extends ManyToOperations<E, A, M> {

  interface EndlessOneToMany<A extends Leaf<M>, M> extends Function<A, Stream<Stream<A>>> {}

  interface EndlessManyToMany<A extends Leaf<M>, M>
      extends Function<Stream<A>, Stream<Stream<A>>> {}

  default E addEndless(
      EndlessManyToMany<A, M> mapToEndless,
      Function<Articioc.Builder<A, M>, Articioc.Builder<A, M>> pipelineDefinition) {
    return this.addEndless(mapToEndless, pipelineDefinition, new StepOptions<>());
  }

  default E addEndless(
      EndlessOneToMany<A, M> mapToEndless,
      Function<Articioc.Builder<A, M>, Articioc.Builder<A, M>> pipelineDefinition) {
    Function<Stream<A>, Stream<Stream<A>>> forEach = e -> e.flatMap(mapToEndless);
    return this.addEndless(forEach::apply, pipelineDefinition, new StepOptions<>());
  }

  default E addEndless(
      EndlessOneToMany<A, M> mapToEndless,
      Function<Articioc.Builder<A, M>, Articioc.Builder<A, M>> pipelineDefinition,
      StepOptions<A, M> options) {
    Function<Stream<A>, Stream<Stream<A>>> forEach = e -> e.flatMap(mapToEndless);
    return this.addEndless(forEach::apply, pipelineDefinition, options);
  }

  default E addEndless(
      EndlessManyToMany<A, M> mapToEndless,
      Function<Articioc.Builder<A, M>, Articioc.Builder<A, M>> pipelineDefinition,
      StepOptions<A, M> options) {
    Step step = options.getEndless().map(StepOptions.EndlessOptions::step).orElse(Step.ENDLESS);

    Provider<A> provider = options
        .getEndless()
        .map(StepOptions.EndlessOptions::provider)
        .orElseGet(EmptyProvider::new);

    boolean shouldCollect =
        options.getEndless().map(StepOptions.EndlessOptions::shouldCollect).orElse(true);

    var builder = new Articioc.Builder<>(
        provider, () -> CompletableFuture.completedFuture(Stream.of()), step);
    var pipeline = pipelineDefinition.apply(builder).end().pipeline(provider);

    Function<Stream<A>, CompletableFuture<Stream<A>>> elaborateChunkByChunk =
        mapToEndless.andThen(chunk -> {
          var previous = chunk.iterator();
          var collector = Stream.<A>of();

          while (previous.hasNext()) {
            Stream<A> next = previous.next();
            var input = CompletableFuture.completedFuture(next.map(e -> (A) e.setStep(step)));

            var chuckResult = pipeline.apply(input).join();

            if (shouldCollect) {
              collector = Stream.concat(collector, chuckResult);
            }
          }

          return CompletableFuture.completedFuture(collector);
        });

    return this.addStepImplementation(elaborateChunkByChunk);
  }
}
