package org.articioc;

import io.vavr.collection.Stream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.articioc.base.interfaces.Provider;
import org.articioc.base.providers.EmptyProvider;
import org.articioc.base.providers.InMemoryProvider;
import org.articioc.base.utils.ChunkStream;
import org.articioc.interfaces.endless.EndlessOperations;
import org.articioc.interfaces.manyTo.ManyToMany;
import org.articioc.interfaces.oneTo.OneToMany;
import org.articioc.interfaces.oneTo.OneToOneAsync;
import org.articioc.tests.models.TestLeaf;
import org.articioc.tests.models.TestStep;
import org.articioc.tests.utils.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ArticiocEndlessOptionsTest {

  private static final String PATH_TO_POTENTIALLY_ENDLESS_FILE = "endless-file.txt";

  private final long totalLinesInFile;

  private final Supplier<CompletableFuture<Stream<TestLeaf>>> trigger =
      () -> Optional.of(TestStep._0)
          .map(TestLeaf::new)
          .map(Stream::of)
          .map(CompletableFuture::completedFuture)
          .orElseThrow();

  private final BiFunction<
          Provider<TestLeaf>,
          StepOptions.EndlessOptions<TestLeaf, TestStep>,
          Articioc<TestLeaf, TestStep>>
      build;

  public ArticiocEndlessOptionsTest() throws IOException {

    this.build = (provider, options) -> {
      var builder = new Articioc.Builder<>(
          provider == null ? new EmptyProvider<>() : provider, trigger, TestStep._0);

      return builder
          .addStep(Utils::enrichStep1)
          .addStep(Utils::enrichStep2)
          .addEndless(
              this.readFileInChunk(),
              b -> b.addStep((OneToOneAsync<TestLeaf>) input -> CompletableFuture.supplyAsync(
                      () -> {
                        System.out.println("Working on: " + input.getStep0());
                        return input;
                      },
                      CompletableFuture.delayedExecutor(
                          (long) (Math.random() * 100 % 10), TimeUnit.MILLISECONDS)))
                  .addStep((OneToMany<TestLeaf>) e -> Stream.of(e.copy(), e.copy()))
                  .addStep((ManyToMany<TestLeaf>) e -> e.distinctBy(TestLeaf::getStep0))
                  .checkpoint(TestStep._1),
              new StepOptions<TestLeaf, TestStep>().setEndless(options))
          .checkpoint(TestStep._1)
          .addStep(Utils::enrichStep4)
          .end();
    };

    try (var lines = Files.lines(pathToTestFile(PATH_TO_POTENTIALLY_ENDLESS_FILE))) {
      this.totalLinesInFile = lines.count();
    }
  }

  @Test
  public void basicScenario() {
    var pipeline = this.build
        .apply(null, new StepOptions.EndlessOptions<>(null, null, null))
        .pipeline();

    var result = pipeline.apply(trigger.get()).join().toList();

    Assertions.assertEquals(
        IntStream.range(0, (int) this.totalLinesInFile)
            .mapToObj(String::valueOf)
            .toList(),
        result.map(TestLeaf::getStep0).toJavaList());

    TestLeaf first = result.head();
    Assertions.assertEquals("0", first.getStep0());

    Assertions.assertTrue(result.forAll(e -> e.getStep3() == null));
    Assertions.assertTrue(result.forAll(e -> e.getStep4().equals("Step 4 here!")));

    TestLeaf last = result.last();
    Assertions.assertEquals(String.valueOf((this.totalLinesInFile - 1)), last.getStep0());
  }

  @Test
  public void shouldNotCollectOutput() {
    var provider = new InMemoryProvider<TestLeaf>();
    var pipeline = this.build
        .apply(provider, new StepOptions.EndlessOptions<>(provider, null, false))
        .pipeline();

    var result = pipeline.apply(trigger.get()).join().toList();

    Assertions.assertEquals(0, result.length());
    Assertions.assertEquals(this.totalLinesInFile, provider.getQueue().length());
  }

  private EndlessOperations.EndlessOneToMany<TestLeaf, TestStep> readFileInChunk() {
    return readFileInChunk(pathToTestFile(PATH_TO_POTENTIALLY_ENDLESS_FILE));
  }

  private EndlessOperations.EndlessOneToMany<TestLeaf, TestStep> readFileInChunk(Path pathToFile) {
    return input -> {
      try {
        var lines = Files.lines(pathToFile).map(l -> input.copy().setStep0(l));
        var chunked = ChunkStream.chunkBy(lines, 5).map(Stream::ofAll);

        return Stream.ofAll(chunked);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private Path pathToTestFile(String file) {
    try {
      return Path.of(getClass().getClassLoader().getResource(file).toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
