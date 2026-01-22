package org.articioc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.vavr.collection.Stream;
import io.vavr.control.Either;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.articioc.base.providers.InMemoryProvider;
import org.articioc.tests.models.TestLeaf;
import org.articioc.tests.models.TestStep;
import org.articioc.tests.utils.Utils;
import org.junit.jupiter.api.Test;

class ArticiocReadAndProcessTest {

  @Test
  void BasicScenario() {
    var record = new TestLeaf(TestStep._0);

    var builder = new Articioc.Builder<>(
        new InMemoryProvider<>(),
        () -> CompletableFuture.completedFuture(Stream.of(record)),
        TestStep._0);

    var articioc = Utils.basic(builder).end();

    var inputRecords = articioc.trigger().join().toList();

    assertEquals(1, inputRecords.size());
    assertEquals(record, inputRecords.headOption().get());

    List<TestLeaf> result = articioc.readThenExecute().join().toJavaList();

    assertEquals(1, result.size());
    var first = result.getFirst();
    assertEquals("OneToOne step", first.getStep0());
    assertEquals("OneToOne step", first.getStep1());

    result = articioc.readThenExecute().join().toJavaList();

    assertEquals(1, result.size());
    first = result.getFirst();
    assertEquals("OneToOne step", first.getStep0());
    assertEquals("OneToOne step, enriched!", first.getStep1());
    assertNull(first.getStep2());
    assertNull(first.getStep3());
    assertNull(first.getStep4());

    result = articioc.readThenExecute().join().toJavaList();

    assertEquals(1, result.size());
    first = result.getFirst();
    assertEquals("OneToOne step", first.getStep0());
    assertEquals("OneToOne step, enriched!", first.getStep1());
    assertEquals("Step 2 here!", first.getStep2());
    assertEquals("Step 3 here!", first.getStep3());
    assertNull(first.getStep4());

    result = articioc.readThenExecute().join().toJavaList();

    assertEquals(2, result.size());
    first = result.getFirst();
    assertEquals("OneToOne step", first.getStep0());
    assertEquals("OneToOne step, enriched!", first.getStep1());
    assertEquals("Step 2 here!", first.getStep2());
    assertEquals("Step 3 here!", first.getStep3());
    assertEquals("Step 4 here!", first.getStep4());

    result = articioc.readThenExecute().join().toJavaList();

    assertEquals(0, result.size());
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
