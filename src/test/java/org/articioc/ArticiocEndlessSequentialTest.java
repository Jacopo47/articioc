package org.articioc;

import io.vavr.collection.Stream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.articioc.base.providers.EmptyProvider;
import org.articioc.base.utils.ChunkStream;
import org.articioc.interfaces.endless.EndlessOperations;
import org.articioc.interfaces.oneTo.OneToOne;
import org.articioc.interfaces.oneTo.OneToOneAsync;
import org.articioc.tests.models.TestLeaf;
import org.articioc.tests.models.TestStep;
import org.articioc.tests.utils.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ArticiocEndlessSequentialTest {

  private static final String PATH_TO_POTENTIALLY_ENDLESS_FILE = "endless-file.txt";

  private final Supplier<CompletableFuture<Stream<TestLeaf>>> trigger =
      () -> Optional.of(TestStep._0)
          .map(TestLeaf::new)
          .map(Stream::of)
          .map(CompletableFuture::completedFuture)
          .orElseThrow();

  public ArticiocEndlessSequentialTest() {}

  private static Map<String, Integer> countByGroup(java.util.stream.Stream<TestLeaf> e) {
    return ChunkStream.chunkBy(e, 101)
        .map(i -> i.collect(Collectors.groupingBy(TestLeaf::getStep1)))
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .map(e1 -> Map.entry(e1.getKey(), e1.getValue().size()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * This test is in order to demonstrate that records are processed sequentially when are generate by a single input.
   */
  @Test
  public void basicScenario() {
    var counter = new AtomicInteger();
    var builder = new Articioc.Builder<>(new EmptyProvider<>(), trigger, TestStep._0);

    Function<TestLeaf, Stream<TestLeaf>> x3 = b -> Stream.of(
        new TestLeaf().setStep1("0"), new TestLeaf().setStep1("1"), new TestLeaf().setStep1("2"));

    EndlessOperations.EndlessOneToMany<TestLeaf, TestStep> readFileInChunk = input -> {
      try {
        counter.incrementAndGet();
        var lines = Files.lines(pathToTestFile(PATH_TO_POTENTIALLY_ENDLESS_FILE))
            .map(l -> input.copy().setStep0(l));
        var chunked = ChunkStream.chunkBy(lines, 5).map(Stream::ofAll);

        return Stream.ofAll(chunked);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };

    var articioc = builder
        .addStep(x3::apply)
        .addEndless(readFileInChunk, b -> b.addStep(
                (OneToOneAsync<TestLeaf>) input -> CompletableFuture.supplyAsync(
                    () -> {
                      System.out.println("Working on: " + input.getStep0());
                      return input;
                    },
                    CompletableFuture.delayedExecutor(
                        (long) (Math.random() * 100 % 10), TimeUnit.MILLISECONDS)))
            .addStep((OneToOne<TestLeaf>) e -> e.setStep1(e.getStep1() + counter.get())))
        .addStep(Utils::enrichStep4)
        .end();

    var pipeline = articioc.pipeline();

    var result = pipeline.apply(trigger.get()).join().toJavaList();

    Assertions.assertEquals(
        Map.of(
            "01", 101,
            "12", 101,
            "23", 101),
        countByGroup(result.stream()));

    var first100 = result.stream().limit(101);
    var middle100 = result.stream().skip(101).limit(101);
    var last100 = result.stream().skip(202);

    Assertions.assertEquals(Map.of("01", 101), countByGroup(first100));
    Assertions.assertEquals(Map.of("12", 101), countByGroup(middle100));
    Assertions.assertEquals(Map.of("23", 101), countByGroup(last100));
  }

  /**
   * This test instead is in order to demonstrate that records are NOT processed sequentially between multiple inputs.
   */
  @Test
  public void sequentialProcessingIsNotGuaranteeOnMultipleRecordsInInput() {
    var counter = new AtomicInteger();
    var builder = new Articioc.Builder<>(new EmptyProvider<>(), trigger, TestStep._0);

    EndlessOperations.EndlessOneToMany<TestLeaf, TestStep> readFileInChunk = input -> {
      try {
        counter.incrementAndGet();
        var lines = Files.lines(pathToTestFile(PATH_TO_POTENTIALLY_ENDLESS_FILE))
            .map(l -> input.copy().setStep0(l));
        var chunked = ChunkStream.chunkBy(lines, 5).map(Stream::ofAll);

        return Stream.ofAll(chunked);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };

    Function<Integer, Executor> delayedExecutor = e -> CompletableFuture.delayedExecutor(
        (long) ((Math.random() * 100 % 10) * e), TimeUnit.MILLISECONDS);

    var articioc = builder
        /* Adding unuseful operations. These steps are added just in order to add some entropy and allow to the records to shuffle. */
        .addStep((OneToOneAsync<TestLeaf>)
            e -> CompletableFuture.supplyAsync(() -> e, delayedExecutor.apply(1)))
        .addStep((OneToOneAsync<TestLeaf>)
            e -> CompletableFuture.supplyAsync(() -> e, delayedExecutor.apply(10)))
        .addStep((OneToOneAsync<TestLeaf>)
            e -> CompletableFuture.supplyAsync(() -> e, delayedExecutor.apply(100)))
        .addStep((OneToOneAsync<TestLeaf>)
            e -> CompletableFuture.supplyAsync(() -> e, delayedExecutor.apply(100)))
        .addEndless(readFileInChunk, b -> b.addStep(
                (OneToOneAsync<TestLeaf>) input -> CompletableFuture.supplyAsync(
                    () -> {
                      // System.out.println("Working on: " + input.getStep0());
                      return input;
                    },
                    CompletableFuture.delayedExecutor(
                        (long) (Math.random() * 100 % 10), TimeUnit.MILLISECONDS)))
            .addStep((OneToOne<TestLeaf>) e -> e.setStep1(e.getStep1() + counter.get())))
        .addStep(Utils::enrichStep4)
        .end();

    var pipeline = articioc.pipeline();

    CompletableFuture<Stream<TestLeaf>> input = CompletableFuture.completedFuture(Stream.of(
            new TestLeaf().setStep1("0").setStep(TestStep._0),
            new TestLeaf().setStep1("1").setStep(TestStep._0),
            new TestLeaf().setStep1("2").setStep(TestStep._0),
            new TestLeaf().setStep1("3").setStep(TestStep._0),
            new TestLeaf().setStep1("4").setStep(TestStep._0),
            new TestLeaf().setStep1("5").setStep(TestStep._0),
            new TestLeaf().setStep1("6").setStep(TestStep._0))
        .map(TestLeaf.class::cast));

    var result = pipeline.apply(input).join().toJavaList();

    Assertions.assertNotEquals(
        Map.of(
            "01", 101,
            "12", 101,
            "23", 101,
            "34", 101,
            "45", 101,
            "56", 101,
            "67", 101),
        countByGroup(result.stream()));
  }

  private Path pathToTestFile(String file) {
    try {
      return Path.of(getClass().getClassLoader().getResource(file).toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
