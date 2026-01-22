package org.articioc;

import static org.awaitility.Awaitility.await;

import io.vavr.collection.Stream;
import io.vavr.control.Either;
import java.util.concurrent.CompletableFuture;
import org.articioc.base.providers.EmptyProvider;
import org.articioc.tests.models.TestLeaf;
import org.articioc.tests.models.TestStep;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ArticiocTest {

  @Test
  void BasicScenario() {
    var record = new TestLeaf(TestStep._0);

    var builder = new Articioc.Builder<>(
        new EmptyProvider<>(),
        () -> CompletableFuture.completedFuture(Stream.of(record)),
        TestStep._0);

    var articioc = builder
        .addStep(this::oneToOne)
        .addStep(this::oneToOneOnStep1)
        .addStep(this::duplicate)
        .checkpoint(TestStep._1)
        .addStep(this::enrichStep1)
        .checkpoint(TestStep._2)
        .addStep(this::enrichStep2)
        .addStep(this::enrichStep3)
        .checkpoint(TestStep._3)
        .addStep(this::enrichStep4)
        .addStep(this::enrichStep4)
        .end();

    var pipeline = articioc.pipeline();

    var future = pipeline.apply(CompletableFuture.completedFuture(Stream.of(record)));
    await().until(future::isDone);

    var result = future.join().toList();
    Assertions.assertEquals(2, result.size());

    var first = result.get(0);

    Assertions.assertEquals("OneToOne step", first.getStep0());
    Assertions.assertEquals("OneToOne step, enriched!", first.getStep1());
    Assertions.assertEquals("Step 2 here!", first.getStep2());
    Assertions.assertEquals("Step 3 here!", first.getStep3());
    Assertions.assertEquals("Step 4 here!", first.getStep4());

    var second = result.get(1);

    Assertions.assertEquals("OneToOne step", second.getStep0());
    Assertions.assertEquals("OneToOne step, enriched!", second.getStep1());
  }

  TestLeaf oneToOne(TestLeaf input) {
    return input.setStep0("OneToOne step");
  }

  TestLeaf oneToOneOnStep1(TestLeaf input) {
    return input.setStep1("OneToOne step");
  }

  Stream<TestLeaf> duplicate(TestLeaf input) {
    return Stream.of(input.copy(), input.copy());
  }

  TestLeaf enrichStep1(TestLeaf input) {
    return input.setStep1(input.getStep1() + ", enriched!");
  }

  Either<Exception, TestLeaf> enrichStep2(TestLeaf input) {
    return Either.right(input.setStep2("Step 2 here!"));
  }

  CompletableFuture<Either<Exception, TestLeaf>> enrichStep3(TestLeaf input) {
    return CompletableFuture.completedFuture(Either.right(input.setStep3("Step 3 here!")));
  }

  CompletableFuture<TestLeaf> enrichStep4(TestLeaf input) {
    return CompletableFuture.completedFuture(input.setStep4("Step 4 here!"));
  }
}
