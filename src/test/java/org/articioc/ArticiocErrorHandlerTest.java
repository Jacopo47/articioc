package org.articioc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.vavr.collection.Stream;
import java.time.DateTimeException;
import java.time.zone.ZoneRulesException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.articioc.base.providers.EmptyProvider;
import org.articioc.tests.models.TestLeaf;
import org.articioc.tests.models.TestStep;
import org.articioc.tests.utils.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ArticiocErrorHandlerTest {
  static class MyException extends RuntimeException {
    public MyException(String message) {
      super(message);
    }
  }

  static class TestLeafWithDynamite extends TestLeaf {
    private final TestStep explodeOn;

    private Supplier<RuntimeException> customException;

    public TestLeafWithDynamite(TestStep step, TestStep explodeOn) {
      super(step);
      this.explodeOn = explodeOn;
    }

    TestStep explodeOnStep() {
      return explodeOn;
    }

    public Optional<Supplier<RuntimeException>> getCustomException() {
      return Optional.ofNullable(customException);
    }

    public TestLeafWithDynamite setCustomException(Supplier<RuntimeException> customException) {
      this.customException = customException;
      return this;
    }
  }

  private final Supplier<CompletableFuture<Stream<TestLeaf>>> trigger =
      () -> Optional.of(TestStep._0)
          .map(TestLeaf::new)
          .map(Stream::of)
          .map(CompletableFuture::completedFuture)
          .orElseThrow();

  private final Articioc<TestLeaf, TestStep> articioc;

  public ArticiocErrorHandlerTest() {
    var builder = new Articioc.Builder<>(new EmptyProvider<>(), trigger, TestStep._0);

    BiFunction<Exception, Stream<TestLeaf>, CompletableFuture<Stream<TestLeaf>>>
        recoverErrorWhenIsMyException = (ex, records) -> switch (ex) {
          case MyException ignored -> CompletableFuture.completedFuture(records);
          default -> CompletableFuture.failedFuture(ex);
        };

    BiFunction<Exception, Stream<TestLeaf>, CompletableFuture<Stream<TestLeaf>>>
        recoverFromNullPointerException = (ex, records) -> switch (ex) {
          case NullPointerException ignored -> CompletableFuture.completedFuture(records);
          default -> CompletableFuture.failedFuture(ex);
        };

    Function<TestLeaf, TestLeaf> explode = e -> {
      if (e instanceof TestLeafWithDynamite withDynamite
          && withDynamite.explodeOnStep() == withDynamite.getStep()) {

        throw withDynamite
            .getCustomException()
            .map(Supplier::get)
            .orElseGet(() -> new MyException("Exploding at step: " + withDynamite.getStep()));
      }

      return e;
    };

    this.articioc = builder
        .addStep(explode::apply)
        .addStep(Utils::oneToOne)
        .addStep(Utils::oneToOneOnStep1)
        .onError(recoverErrorWhenIsMyException)
        .onError(TestStep._3)
        .checkpoint(TestStep._1)
        .addStep(explode::apply)
        .addStep(Utils::enrichStep1)
        .onError(recoverErrorWhenIsMyException)
        .onError(recoverFromNullPointerException, TestStep._3)
        .checkpoint(TestStep._2)
        .addStep(explode::apply)
        .addStep(Utils::enrichStep2)
        .addStep(Utils::enrichStep3)
        .onError(recoverErrorWhenIsMyException)
        .checkpoint(TestStep._3)
        .addStep(explode::apply)
        .addStep(Utils::enrichStep4)
        .onError(recoverErrorWhenIsMyException)
        .onError(recoverFromNullPointerException)
        .end();
  }

  @Test
  void basicScenario_whenNoError_shouldComplete() {
    var result = articioc.pipeline().apply(trigger.get()).join();

    Assertions.assertTrue(result.headOption().isDefined());
  }

  @ParameterizedTest
  @MethodSource("testStepProvider")
  void basicScenario_whenError_shouldRunExceptionHandler(TestStep explodeAt) {
    var result = articioc
        .pipeline()
        .apply(CompletableFuture.completedFuture(
            Stream.of(new TestLeafWithDynamite(TestStep._0, explodeAt))))
        .join();

    Assertions.assertTrue(result.headOption().isDefined());
  }

  @Test
  void basicScenario_whenError_ThrowNullPointerExceptionOnStep1ThenJumpsOnStep3() {
    var result = articioc
        .pipeline()
        .apply(CompletableFuture.completedFuture(
            Stream.of(new TestLeafWithDynamite(TestStep._0, TestStep._1)
                .setCustomException(NullPointerException::new))))
        .join();

    var first = result.headOption().get();

    assertEquals("OneToOne step", first.getStep0());
    assertEquals("OneToOne step", first.getStep1());
    assertNull(first.getStep2());
    assertNull(first.getStep3());
    assertEquals("Step 4 here!", first.getStep4());
  }

  @ParameterizedTest
  @MethodSource("variousDifferentExceptions")
  void basicScenario_whenErrorWithOnlyGoToStep_ShouldGoToThatStepInAnyScenario(
      RuntimeException exception) {
    var result = articioc
        .pipeline()
        .apply(CompletableFuture.completedFuture(
            Stream.of(new TestLeafWithDynamite(TestStep._0, TestStep._0)
                .setCustomException(() -> exception))))
        .join();

    var first = result.headOption().get();

    assertNull(first.getStep0());
    assertNull(first.getStep1());
    assertNull(first.getStep2());
    assertNull(first.getStep3());
    assertEquals("Step 4 here!", first.getStep4());
  }

  private static Stream<RuntimeException> variousDifferentExceptions() {
    return Stream.of(
        new IndexOutOfBoundsException(),
        new RuntimeException(),
        new ZoneRulesException(""),
        new DateTimeException(""));
  }

  private static Stream<TestStep> testStepProvider() {
    return Stream.of(TestStep._0, TestStep._1, TestStep._2, TestStep._3);
  }
}
