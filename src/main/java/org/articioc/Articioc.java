package org.articioc;

import static org.articioc.base.utils.Futures.whenAll;

import io.vavr.collection.List;
import io.vavr.collection.Stream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.articioc.base.Leaf;
import org.articioc.base.LeafCarrier;
import org.articioc.base.Step;
import org.articioc.base.interfaces.CommitOperation;
import org.articioc.base.interfaces.Provider;
import org.articioc.base.interfaces.ProviderAsExecutor;
import org.articioc.base.interfaces.RollbackOperation;
import org.articioc.base.models.CommitOperationOptions;
import org.articioc.base.models.RollbackOperationOptions;
import org.articioc.base.providers.EmptyProvider;
import org.articioc.exceptions.SkipNextSteps;
import org.articioc.exceptions.SkipPipelineSinceThereAreNoRecordsToProcess;
import org.articioc.interfaces.ErrorPipeline;
import org.articioc.interfaces.Pipeline;
import org.articioc.interfaces.endless.EndlessOperations;
import org.articioc.interfaces.errors.OnErrorOperations;
import org.articioc.interfaces.executors.AutonomousExecutor;
import org.articioc.interfaces.executors.ToPipeline;
import org.articioc.interfaces.manyTo.ManyToOperations;
import org.articioc.interfaces.oneTo.OneToOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PipelineStep<A, S> {
  private final S from;
  private final S to;
  private Pipeline<A> pipeline;
  private ErrorPipeline<A> errorPipeline;

  public PipelineStep(S from, S to, Pipeline<A> pipeline, ErrorPipeline<A> errorPipeline) {
    this.from = Objects.requireNonNull(from);
    this.to = Objects.requireNonNull(to);
    this.pipeline = Objects.requireNonNull(pipeline);
    this.errorPipeline = Objects.requireNonNull(errorPipeline);
  }

  public S getFrom() {
    return from;
  }

  public S getTo() {
    return to;
  }

  public Pipeline<A> getPipeline() {
    return pipeline;
  }

  public ErrorPipeline<A> getErrorPipeline() {
    return errorPipeline;
  }

  public PipelineStep<A, S> andThen(Pipeline<A> next) {
    this.pipeline = pipeline.andThen(next);

    return this;
  }
}

public class Articioc<A extends Leaf<M>, M> implements ToPipeline<A, M>, AutonomousExecutor<A, M> {

  private static final Logger logger = LoggerFactory.getLogger(Articioc.class);
  private final Provider<A> provider;
  private final Supplier<CompletableFuture<Stream<A>>> trigger;
  private final Step firstStep;
  private final List<PipelineStep<A, Step>> builder;

  private Articioc(
      Provider<A> provider,
      Supplier<CompletableFuture<Stream<A>>> trigger,
      Step firstStep,
      List<PipelineStep<A, Step>> builder) {
    this.provider = provider;
    this.trigger = trigger;
    this.firstStep = firstStep;
    this.builder = builder;

    if (provider instanceof ProviderAsExecutor<A> asExecutor) {
      asExecutor.setPipeline(this::applyPipelineToMessage);
    }
  }

  @Override
  public CompletableFuture<Stream<A>> readThenExecute() {
    return provider.read().thenCompose(e -> this.applyPipelineToMessage(e, provider));
  }

  @Override
  public CompletableFuture<Stream<A>> trigger(Provider<A> provider) {
    Function<Stream<A>, Stream<A>> applyFirstStepToRecord =
        r -> r.map(e -> (A) e.setStep(firstStep));

    return trigger.get().thenApply(applyFirstStepToRecord).thenCompose(provider::write);
  }

  @Override
  public CompletableFuture<Stream<A>> trigger() {
    return trigger(this.provider);
  }

  @Override
  public Pipeline<A> pipeline() {
    return pipeline(new EmptyProvider<>());
  }

  @Override
  public Pipeline<A> pipeline(Provider<A> customProvider) {
    Function<Stream<A>, CompletableFuture<Stream<A>>> skipIfThereAreNoRecords = records -> {
      if (records.isEmpty())
        return CompletableFuture.failedFuture(
            new SkipPipelineSinceThereAreNoRecordsToProcess(Stream.empty()));

      return CompletableFuture.completedFuture(records);
    };

    Function<Stream<A>, CompletableFuture<Stream<A>>> skipIfAllRecordsAreFinal = records -> {
      if (records.forAll(e -> e.getStep().isFinal()))
        return CompletableFuture.failedFuture(
            new SkipPipelineSinceThereAreNoRecordsToProcess(records));

      return CompletableFuture.completedFuture(records);
    };

    Function<Throwable, CompletableFuture<Stream<A>>> recoverFromSkip =
        ex -> switch (ex.getCause()) {
          case SkipPipelineSinceThereAreNoRecordsToProcess skip ->
            CompletableFuture.completedFuture((Stream<A>) skip.getRecords());
          default -> CompletableFuture.failedFuture(ex);
        };

    return input -> input
        .thenCompose(skipIfThereAreNoRecords)
        .thenCompose(skipIfAllRecordsAreFinal)
        .thenApply(e -> e.map(LeafCarrier::from))
        .thenCompose(e -> this.applyPipelineToMessage(e, customProvider))
        .thenCompose(e -> pipeline(customProvider).apply(CompletableFuture.completedFuture(e)))
        .exceptionallyCompose(recoverFromSkip);
  }

  private CompletableFuture<Stream<A>> applyPipelineToMessage(Stream<LeafCarrier<A>> messages) {
    return this.applyPipelineToMessage(messages, provider);
  }

  private CompletableFuture<Stream<A>> applyPipelineToMessage(
      Stream<LeafCarrier<A>> messages, Provider<A> provider) {
    Function<LeafCarrier<A>, Stream<PipelineWithMessage<A>>> linkPipelineToEachMessage = input -> {
      Function<Step, Optional<PipelineStep<A, Step>>> find =
          b -> builder.find(r -> r.getFrom().equals(b)).toJavaOptional();

      var step = Optional.ofNullable(input).map(LeafCarrier::getData).map(Leaf::getStep);

      return step.flatMap(find)
          .map(e ->
              PipelineWithMessage.from(input, e.getPipeline(), e.getErrorPipeline(), e.getTo()))
          .map(Stream::of)
          .orElseGet(() -> {
            logger.warn(
                "Pipeline not found for message with step: {}. It will be discarded.", step);
            return Stream.of();
          });
    };

    Function<PipelineWithMessage<A>, CompletableFuture<Stream<A>>> executePipeline = p -> {
      final Function<Stream<A>, CompletableFuture<Stream<A>>> writeOnProvider = input -> {
        Map<Boolean, java.util.List<A>> groupedByFinal =
            input.collect(Collectors.groupingBy(e -> e.getStep().isFinal()));
        var finalRecords = Optional.of(groupedByFinal)
            .map(e -> e.get(true))
            .map(Stream::ofAll)
            .orElseGet(Stream::empty);
        var intermediateRecords = Optional.of(groupedByFinal)
            .map(e -> e.get(false))
            .map(Stream::ofAll)
            .orElseGet(Stream::of);

        var groupedByReadonly = Optional.of(intermediateRecords)
            .map(r -> r.collect(Collectors.groupingBy(e -> e.getStep().isReadonly())));

        var readonlyRecords =
            groupedByReadonly.map(e -> e.get(true)).map(Stream::ofAll).orElseGet(Stream::of);

        var toBeWrittenRecords =
            groupedByReadonly.map(e -> e.get(false)).map(Stream::ofAll).orElseGet(Stream::of);

        var written = provider.write(toBeWrittenRecords);

        return written
            .thenApply(s -> Stream.concat(s, finalRecords))
            .thenApply(s -> Stream.concat(s, readonlyRecords));
      };

      final Function<Stream<A>, CompletableFuture<Stream<A>>> commit = recordsInOutput -> p.commit()
          .map(commitOperation ->
              commitOperation.commit(new CommitOperationOptions<>(recordsInOutput, p.to())))
          .map(ok -> ok.thenApply(ignore -> recordsInOutput))
          .orElseGet(() -> CompletableFuture.completedFuture(recordsInOutput));

      final Function<Throwable, CompletableFuture<Stream<A>>> rollback = cause -> p.rollback()
          .map(rollbackOperation ->
              rollbackOperation.rollback(new RollbackOperationOptions<>(cause, p.to())))
          .map(ok -> ok.thenCompose(ignore -> CompletableFuture.<Stream<A>>failedFuture(cause)))
          .orElseGet(() -> CompletableFuture.failedFuture(cause));

      final Function<Throwable, CompletableFuture<Stream<A>>> recoverFromError = ex -> {
        if (p.errorPipeline() == null) return CompletableFuture.failedFuture(ex);

        var innerException =
            switch (ex) {
              case CompletionException ce -> ce.getCause();
              default -> ex;
            };

        Exception cause =
            switch (innerException) {
              case Exception e -> e;
              default -> new RuntimeException(innerException);
            };

        return p.errorPipeline().apply(cause, Stream.of(p.record()), p.to());
      };

      final Function<Throwable, CompletableFuture<Stream<A>>> recoverFromSkipSteps =
          ex -> switch (ex) {
            case CompletionException ce
            when ce.getCause() instanceof SkipNextSteps skip ->
              CompletableFuture.completedFuture((Stream<A>) skip.getRecords());
            default -> CompletableFuture.failedFuture(ex);
          };

      var input = CompletableFuture.completedFuture(Stream.of(p.record()));
      return p.pipeline()
          .apply(input)
          .exceptionallyCompose(recoverFromError)
          .exceptionallyCompose(recoverFromSkipSteps)
          .thenCompose(writeOnProvider)
          .thenCompose(commit)
          .exceptionallyCompose(rollback);
    };

    Stream<PipelineWithMessage<A>> pipelines = messages.flatMap(linkPipelineToEachMessage);

    var futures = pipelines.map(executePipeline);

    return whenAll(futures).exceptionallyCompose(ex -> {
      logger.atError().log("Pipeline was executed and encountered and error.", ex);

      return CompletableFuture.completedFuture(Stream.of());
    });
  }

  public static class Builder<A extends Leaf<M>, M>
      implements OneToOperations<Articioc.Builder<A, M>, A, M>,
          ManyToOperations<Articioc.Builder<A, M>, A, M>,
          OnErrorOperations<Articioc.Builder<A, M>, A, M>,
          EndlessOperations<Articioc.Builder<A, M>, A, M> {

    private final Provider<A> provider;
    private final Supplier<CompletableFuture<Stream<A>>> trigger;
    private final Step firstStep;

    private List<PipelineStep<A, Step>> builder;
    private Step step;
    private Pipeline<A> currentStep;
    private ErrorPipeline<A> currentErrorPipeline;

    public Builder(
        Provider<A> provider, Supplier<CompletableFuture<Stream<A>>> trigger, Step firstStep) {
      this.provider = provider;
      this.trigger = trigger;
      this.firstStep = firstStep;

      this.step = firstStep;

      this.builder = List.empty();
      this.currentStep = Pipeline.identity();
      this.currentErrorPipeline = ErrorPipeline.identity();
    }

    public Builder<A, M> checkpoint(Step next) {
      return this.checkpoint(next, new StepOptions<>());
    }

    public Builder<A, M> checkpoint(Step next, StepOptions<A, M> options) {
      var isReadonly =
          options.getCheckpoint().map(StepOptions.CheckpointOptions::readonly).orElse(false);

      next.setReadonly(isReadonly);

      this.builder =
          builder.append(new PipelineStep<>(this.step, next, currentStep, currentErrorPipeline));

      this.step = next;
      this.currentStep = Pipeline.identity();
      this.currentErrorPipeline = ErrorPipeline.identity();

      return this;
    }

    public Articioc<A, M> end() {
      BiFunction<Step, CompletableFuture<Stream<A>>, CompletableFuture<Stream<A>>> setStep =
          (step, input) -> input.thenApply(r -> r.map(e -> (A) e.setStep(step)));

      Function<PipelineStep<A, Step>, PipelineStep<A, Step>> setStepOnEachPipeline =
          input -> input.andThen(e -> setStep.apply(input.getTo(), e));

      this.builder = builder.map(setStepOnEachPipeline);

      var lastStep = new PipelineStep<>(this.step, Step.FINAL, currentStep, currentErrorPipeline)
          .andThen(e -> setStep.apply(Step.FINAL, e));

      this.builder = builder.append(lastStep);

      return new Articioc<>(this.provider, this.trigger, this.firstStep, this.builder);
    }

    @Override
    public Builder<A, M> addStepOnSingleElement(Function<A, CompletableFuture<Stream<A>>> step) {
      Function<Stream<A>, CompletableFuture<Stream<A>>> applyStepToEachPreviousResult = input -> {
        var futures = input.map(step);

        return whenAll(futures);
      };

      return this.addStepImplementation(applyStepToEachPreviousResult);
    }

    @Override
    public Builder<A, M> addStepImplementation(
        Function<Stream<A>, CompletableFuture<Stream<A>>> step) {
      this.currentStep = this.currentStep.andThen(previous -> previous.thenCompose(step));
      return this;
    }

    @Override
    public Builder<A, M> onError(
        BiFunction<Exception, Stream<A>, CompletableFuture<Stream<A>>> exceptionHandler,
        Step goToStep) {
      if (exceptionHandler == null && goToStep == null)
        throw new RuntimeException(
            "At least one between: exceptionHandler or goToStep must be provided in error handling");

      var prev = this.currentErrorPipeline;
      this.currentErrorPipeline =
          (ex, i, step) -> prev.apply(ex, i, step).exceptionallyCompose(completionException -> {
            var original = (Exception)
                switch (completionException) {
                  case CompletionException ce -> ce.getCause();
                  default -> completionException;
                };

            if (original instanceof SkipNextSteps skip) {
              return CompletableFuture.failedFuture(skip);
            }

            var handled = exceptionHandler == null
                ? CompletableFuture.completedFuture(i)
                : exceptionHandler.apply(original, i);

            return handled.thenCompose(r -> {
              var target = goToStep == null ? step : goToStep;

              Stream<A> records = r.map(e -> (A) e.setStep(target));

              return CompletableFuture.failedFuture(new SkipNextSteps(records));
            });
          });

      return this;
    }
  }

  private record PipelineWithMessage<A>(
      A record,
      Optional<CommitOperation<A>> commit,
      Optional<RollbackOperation<A>> rollback,
      Pipeline<A> pipeline,
      ErrorPipeline<A> errorPipeline,
      Step to) {
    public static <A> PipelineWithMessage<A> from(
        LeafCarrier<A> input, Pipeline<A> pipeline, ErrorPipeline<A> errorPipeline, Step to) {
      return new PipelineWithMessage<>(
          input.getData(),
          input.getCommitOperation(),
          input.getRollbackOperation(),
          pipeline,
          errorPipeline,
          to);
    }
  }
}
