package org.articioc;

import io.vavr.collection.Stream;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.articioc.base.providers.EmptyProvider;
import org.articioc.base.utils.ChunkStream;
import org.articioc.interfaces.endless.EndlessOperations;
import org.articioc.interfaces.manyTo.ManyToMany;
import org.articioc.interfaces.oneTo.OneToMany;
import org.articioc.interfaces.oneTo.OneToOne;
import org.articioc.interfaces.oneTo.OneToOneAsync;
import org.articioc.tests.models.TestLeaf;
import org.articioc.tests.models.TestStep;
import org.articioc.tests.utils.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ArticiocEndlessTest {

  private static final String PATH_TO_POTENTIALLY_ENDLESS_FILE = "endless-file.txt";

  private final long totalLinesInFile;

  private final Supplier<CompletableFuture<Stream<TestLeaf>>> trigger =
      () -> Optional.of(TestStep._0)
          .map(TestLeaf::new)
          .map(Stream::of)
          .map(CompletableFuture::completedFuture)
          .orElseThrow();

  private final Articioc<TestLeaf, TestStep> articioc;

  public ArticiocEndlessTest() throws IOException {
    var builder = new Articioc.Builder<>(new EmptyProvider<>(), trigger, TestStep._0);

    this.articioc = builder
        .addStep(Utils::enrichStep1)
        .addStep(Utils::enrichStep2)
        .addEndless(this.readFileInChunk(), b -> b.addStep(
                (OneToOneAsync<TestLeaf>) input -> CompletableFuture.supplyAsync(
                    () -> {
                      System.out.println("Working on: " + input.getStep0());
                      return input;
                    },
                    CompletableFuture.delayedExecutor(
                        (long) (Math.random() * 100 % 10), TimeUnit.MILLISECONDS)))
            .addStep((OneToMany<TestLeaf>) e -> Stream.of(e.copy(), e.copy())))
        .addStep((ManyToMany<TestLeaf>) e -> Stream.ofAll(e).distinctBy(TestLeaf::getStep0))
        .addStep(Utils::enrichStep4)
        .end();

    try (var lines = Files.lines(pathToTestFile(PATH_TO_POTENTIALLY_ENDLESS_FILE))) {
      this.totalLinesInFile = lines.count();
    }
  }

  @Test
  public void basicScenario() {
    var pipeline = articioc.pipeline();

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
  public void isNotCollectinOutputWhenSet() {
    var builder = new Articioc.Builder<>(new EmptyProvider<>(), trigger, TestStep._0);

    var pipeline = builder
        .addStep(Utils::enrichStep1)
        .addStep(Utils::enrichStep2)
        .addEndless(this.readFileInChunk(), b -> b.addStep(
                (OneToOneAsync<TestLeaf>) input -> CompletableFuture.supplyAsync(
                    () -> {
                      System.out.println("Working on: " + input.getStep0());
                      return input;
                    },
                    CompletableFuture.delayedExecutor(
                        (long) (Math.random() * 100 % 10), TimeUnit.MILLISECONDS)))
            .addStep((OneToMany<TestLeaf>) e -> Stream.of(e.copy(), e.copy())))
        .addStep((ManyToMany<TestLeaf>) e -> Stream.ofAll(e).distinctBy(TestLeaf::getStep0))
        .addStep(Utils::enrichStep4)
        .end()
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
  public void verifyThatFileIsClosedAndDeleteOnlyWhenTotallyRead(@TempDir Path workingDir)
      throws IOException {
    var builder = new Articioc.Builder<>(new EmptyProvider<>(), trigger, TestStep._0);

    var workingFile = workingDir.resolve("working-file.txt");
    Files.copy(pathToTestFile(PATH_TO_POTENTIALLY_ENDLESS_FILE), workingFile);

    OneToOne<TestLeaf> removeFile = i -> {
      try {
        Files.delete(workingFile);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      return i;
    };

    OneToOne<TestLeaf> verifyThatFileDoesNotExistsAnymore = i -> {
      if (Files.exists(workingFile))
        throw new RuntimeException("Working file should not exists anymore");

      return i;
    };

    var pipeline = builder
        .addStep(Utils::enrichStep1)
        .addStep(Utils::enrichStep2)
        .addEndless(this.readFileInChunk(workingFile), b -> b.addStep(
                (OneToOneAsync<TestLeaf>) input -> CompletableFuture.supplyAsync(
                    () -> {
                      System.out.println("Working on: " + input.getStep0());
                      return input;
                    },
                    CompletableFuture.delayedExecutor(
                        (long) (Math.random() * 100 % 10), TimeUnit.MILLISECONDS)))
            .addStep((OneToMany<TestLeaf>) e -> Stream.of(e.copy(), e.copy())))
        .addStep((ManyToMany<TestLeaf>) e -> {
          var result = e.toJavaList();
          Assertions.assertEquals(202, result.size());

          return e.headOption().toStream();
        })
        .addStep(removeFile)
        .addStep(verifyThatFileDoesNotExistsAnymore)
        .addStep(Utils::enrichStep4)
        .end()
        .pipeline();

    var result = pipeline.apply(trigger.get()).join().toList();

    Assertions.assertEquals(1, result.length());

    var head = result.head();
    TestLeaf first = result.head();
    Assertions.assertEquals("0", first.getStep0());

    Assertions.assertNull(head.getStep3());
    Assertions.assertEquals("Step 4 here!", head.getStep4());
  }

  @Test
  public void readFileByChunkAndPopulateFileInOutput(@TempDir Path workingDir) throws IOException {
    var builder = new Articioc.Builder<>(new EmptyProvider<>(), trigger, TestStep._0);

    var workingFile = workingDir.resolve("working-file.txt");
    var outputFile = workingDir.resolve("output.txt");
    Files.copy(pathToTestFile(PATH_TO_POTENTIALLY_ENDLESS_FILE), workingFile);

    OneToOne<TestLeaf> removeFile = i -> {
      try {
        Files.delete(workingFile);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      return i;
    };

    OneToOne<TestLeaf> verifyThatFileDoesNotExistsAnymore = i -> {
      if (Files.exists(workingFile))
        throw new RuntimeException("Working file should not exists anymore");

      return i;
    };

    var pipeline = builder
        .addStep(Utils::oneToOneOnStep1)
        .addStep(Utils::enrichStep1)
        .addStep(Utils::enrichStep2)
        .addEndless(this.readFileInChunk(workingFile), b -> b.addStep(
                (OneToMany<TestLeaf>) e -> Stream.of(e.copy(), e.copy()))
            .addStep((ManyToMany<TestLeaf>) result -> {
              BiFunction<String, String, String> join = (p, n) -> p + n;
              var lines = result
                  .map(e -> e.getStep0() + " - " + e.getStep1() + System.lineSeparator())
                  .reduce(join);

              try {
                Files.writeString(
                    outputFile, lines, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }

              return result;
            }))
        .addStep((ManyToMany<TestLeaf>) e -> {
          var result = e.toJavaList();
          Assertions.assertEquals(202, result.size());

          return e.headOption().toStream();
        })
        .addStep(removeFile)
        .addStep(verifyThatFileDoesNotExistsAnymore)
        .addStep(Utils::enrichStep4)
        .end()
        .pipeline();

    var result = pipeline.apply(trigger.get()).join().toList();

    Assertions.assertEquals(1, result.length());

    var head = result.head();
    TestLeaf first = result.head();
    Assertions.assertEquals("0", first.getStep0());

    Assertions.assertNull(head.getStep3());
    Assertions.assertEquals("Step 4 here!", head.getStep4());

    var lines = Stream.ofAll(Files.lines(outputFile));

    Assertions.assertEquals(202, lines.length());
    Assertions.assertEquals("0 - OneToOne step, enriched!", lines.head());

    Assertions.assertEquals((totalLinesInFile - 1) + " - OneToOne step, enriched!", lines.last());
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
