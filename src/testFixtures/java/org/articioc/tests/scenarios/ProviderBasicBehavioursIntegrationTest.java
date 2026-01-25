package org.articioc.tests.scenarios;

import io.vavr.collection.Stream;
import io.vavr.control.Either;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.articioc.Articioc;
import org.articioc.base.interfaces.Provider;
import org.articioc.interfaces.oneTo.OneToOne;
import org.articioc.tests.models.TestStep;
import org.junit.jupiter.api.*;

public interface ProviderBasicBehavioursIntegrationTest<B extends BasicProviderTestLeaf<M>, M>
    extends BasicOperationsProviderIntegrationTest<B, M> {

  @AfterEach
  default void afterEach() throws Exception {
    provider().close();;
  }

  @Test
  default void basicScenario() throws Exception {
    Provider<B> provider = this.provider();
    var builder = new Articioc.Builder<>(
        (Provider<BasicProviderTestLeaf<M>>) provider,
        () -> CompletableFuture.completedFuture(Stream.of(first())),
        TestStep._0);

    var articioc = this.basic(builder).end();

    var initRecords = articioc.trigger().join().toList();
    Assertions.assertFalse(initRecords.isEmpty());

    List<BasicProviderTestLeaf<M>> result = articioc.readThenExecute().join().toJavaList();

    Assertions.assertEquals(1, result.size());
    var first = result.getFirst();
    Assertions.assertEquals("OneToOne step", first.getStep0());
    Assertions.assertEquals("OneToOne step", first.getStep1());

    result = articioc.readThenExecute().join().toJavaList();

    Assertions.assertEquals(1, result.size());
    first = result.getFirst();
    Assertions.assertEquals("OneToOne step", first.getStep0());
    Assertions.assertEquals("OneToOne step, enriched!", first.getStep1());
    Assertions.assertNull(first.getStep2());
    Assertions.assertNull(first.getStep3());
    Assertions.assertNull(first.getStep4());

    result = articioc.readThenExecute().join().toJavaList();

    Assertions.assertEquals(1, result.size());
    first = result.getFirst();
    Assertions.assertEquals("OneToOne step", first.getStep0());
    Assertions.assertEquals("OneToOne step, enriched!", first.getStep1());
    Assertions.assertEquals("Step 2 here!", first.getStep2());
    Assertions.assertEquals("Step 3 here!", first.getStep3());
    Assertions.assertNull(first.getStep4());

    result = articioc.readThenExecute().join().toJavaList();

    Assertions.assertEquals(2, result.size());
    first = result.getFirst();
    Assertions.assertEquals("OneToOne step", first.getStep0());
    Assertions.assertEquals("OneToOne step, enriched!", first.getStep1());
    Assertions.assertEquals("Step 2 here!", first.getStep2());
    Assertions.assertEquals("Step 3 here!", first.getStep3());
    Assertions.assertEquals("Step 4 here!", first.getStep4());

    result = articioc.readThenExecute().join().toJavaList();

    Assertions.assertEquals(0, result.size());

    provider().close();
  }

  @Test
  default void withErrorAtFirstTryButShouldReprocess() throws Exception {
    Provider<B> provider = this.provider();
    var builder = new Articioc.Builder<>(
        (Provider<BasicProviderTestLeaf<M>>) provider,
        () -> Stream.of(first()),
        TestStep._0);

    AtomicInteger tentative = new AtomicInteger();

    var articioc = this.basic(builder)
        .addStep((OneToOne<BasicProviderTestLeaf<M>>) e -> {
          if (tentative.getAndIncrement() == 0) throw new RuntimeException("You should not pass at first try");

          return e;
        })
        .end();

    var initRecords = articioc.trigger().join().toList();
    Assertions.assertFalse(initRecords.isEmpty());

    List<BasicProviderTestLeaf<M>> result = articioc.readThenExecute().join().toJavaList();

    Assertions.assertEquals(1, result.size());
    var first = result.getFirst();
    Assertions.assertEquals("OneToOne step", first.getStep0());
    Assertions.assertEquals("OneToOne step", first.getStep1());

    result = articioc.readThenExecute().join().toJavaList();

    Assertions.assertEquals(1, result.size());
    first = result.getFirst();
    Assertions.assertEquals("OneToOne step", first.getStep0());
    Assertions.assertEquals("OneToOne step, enriched!", first.getStep1());
    Assertions.assertNull(first.getStep2());
    Assertions.assertNull(first.getStep3());
    Assertions.assertNull(first.getStep4());

    result = articioc.readThenExecute().join().toJavaList();

    Assertions.assertEquals(1, result.size());
    first = result.getFirst();
    Assertions.assertEquals("OneToOne step", first.getStep0());
    Assertions.assertEquals("OneToOne step, enriched!", first.getStep1());
    Assertions.assertEquals("Step 2 here!", first.getStep2());
    Assertions.assertEquals("Step 3 here!", first.getStep3());
    Assertions.assertNull(first.getStep4());

    result = articioc.readThenExecute().join().toJavaList();

    /* Expecting error here at first try... */
    Assertions.assertEquals(0, result.size());

    /* So let's try again... */
    result = articioc.readThenExecute().join().toJavaList();
    Assertions.assertEquals(2, result.size());
    first = result.getFirst();
    Assertions.assertEquals("OneToOne step", first.getStep0());
    Assertions.assertEquals("OneToOne step, enriched!", first.getStep1());
    Assertions.assertEquals("Step 2 here!", first.getStep2());
    Assertions.assertEquals("Step 3 here!", first.getStep3());
    Assertions.assertEquals("Step 4 here!", first.getStep4());

    result = articioc.readThenExecute().join().toJavaList();

    Assertions.assertEquals(0, result.size());

    provider().close();
  }

  private Articioc.Builder<BasicProviderTestLeaf<M>, M> basic(
      Articioc.Builder<BasicProviderTestLeaf<M>, M> builder) {
    return builder
        .addStep(this::oneToOne)
        .addStep(this::oneToOneOnStep1)
        .checkpoint(TestStep._1)
        .addStep(this::enrichStep1)
        .checkpoint(TestStep._2)
        .addStep(this::enrichStep2)
        .addStep(this::enrichStep3)
        .checkpoint(TestStep._3)
        .addStep(this::duplicate)
        .addStep(this::enrichStep4)
        .addStep(this::enrichStep4);
  }

  private BasicProviderTestLeaf<M> oneToOne(BasicProviderTestLeaf<M> input) {
    return input.setStep0("OneToOne step");
  }

  private BasicProviderTestLeaf<M> oneToOneOnStep1(BasicProviderTestLeaf<M> input) {
    return input.setStep1("OneToOne step");
  }

  private Stream<BasicProviderTestLeaf<M>> duplicate(BasicProviderTestLeaf<M> input) {
    return Stream.of(input.copy(), input.copy());
  }

  private BasicProviderTestLeaf<M> enrichStep1(BasicProviderTestLeaf<M> input) {
    return input.setStep1(input.getStep1() + ", enriched!");
  }

  private Either<Exception, BasicProviderTestLeaf<M>> enrichStep2(BasicProviderTestLeaf<M> input) {
    return Either.right(input.setStep2("Step 2 here!"));
  }

  private CompletableFuture<Either<Exception, BasicProviderTestLeaf<M>>> enrichStep3(
      BasicProviderTestLeaf<M> input) {
    return CompletableFuture.completedFuture(Either.right(input.setStep3("Step 3 here!")));
  }

  private CompletableFuture<BasicProviderTestLeaf<M>> enrichStep4(BasicProviderTestLeaf<M> input) {
    return CompletableFuture.completedFuture(input.setStep4("Step 4 here!"));
  }
}
