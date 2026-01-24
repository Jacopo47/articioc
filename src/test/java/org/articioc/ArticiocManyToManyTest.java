package org.articioc;

import io.vavr.collection.Stream;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.articioc.base.providers.EmptyProvider;
import org.articioc.tests.models.TestLeaf;
import org.articioc.tests.models.TestStep;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ArticiocManyToManyTest {

  @Test
  void BasicScenario_VerifyThatGeneratedMessageAreTreatTogetherInTheSamePipeline() {
    var record = new TestLeaf(TestStep._0);

    var input = CompletableFuture.completedFuture(Stream.of(record));

    var builder = new Articioc.Builder<>(new EmptyProvider<>(), () -> input, TestStep._0);

    var articioc = builder
        .addStep(this::print)
        .checkpoint(TestStep._1)
        .addStep(this::duplicate)
        .addStep(this::explodeOneToMany)
        .addStep(this::keepOnlySingleCharacters)
        .addStep(this::distinct)
        .addStep(this::orderBy)
        .end();

    var result = articioc.pipeline().apply(input).join().toList();

    Assertions.assertEquals(4, result.size());

    var letters = result.map(TestLeaf::getStep0).toJavaList();

    Assertions.assertEquals(List.of("a", "b", "c", "d"), letters);
  }

  @Test
  void BasicScenario_VerifyThatGeneratedMessageAreTreatIndependentlyWhenCheckpointIsPerformed() {
    var record = new TestLeaf(TestStep._0);

    var input = CompletableFuture.completedFuture(Stream.of(record));

    var builder = new Articioc.Builder<>(new EmptyProvider<>(), () -> input, TestStep._0);

    var articioc = builder
        .addStep(this::print)
        .checkpoint(TestStep._1)
        .addStep(this::duplicate)
        .addStep(this::explodeOneToMany)
        .addStep(this::keepOnlySingleCharacters)
        .addStep(this::distinct)
        /* Setting a checkpoint just before the order by will force the library to store the records independently
         by each other into the provider.
        That means that, from the next step (orderBy in this scenario) messages are then treated independently
        so ordering will have no effect since under the hood the library will perform these call:
         orderBy(["d"])
         then orderBy(["b"])
         then orderBy(["a"])
         then orderBy(["c"])

         and then, since it's the last operation, collect all the result together into: ["d", "b", "a", "c"].

        That's why, differently from the method: BasicScenario_VerifyThatGeneratedMessageAreTreatTogetherInTheSamePipeline,
         records are actually not ordered. */
        .checkpoint(TestStep._2)
        .addStep(this::orderBy)
        .end();

    var result = articioc.pipeline().apply(input).join().toList();

    Assertions.assertEquals(4, result.size());

    var letters = result.map(TestLeaf::getStep0).toJavaList();

    Assertions.assertEquals(List.of("d", "b", "a", "c"), letters);
  }

  TestLeaf print(TestLeaf input) {
    System.out.printf("Elaborating record: %s%n", input);

    return input;
  }

  Stream<TestLeaf> explodeOneToMany(TestLeaf input) {
    return Stream.of(
        new TestLeaf(TestStep._0).setStep0("d"),
        new TestLeaf(TestStep._0).setStep0("b"),
        new TestLeaf(TestStep._0).setStep0("a"),
        new TestLeaf(TestStep._0).setStep0("c"),
        new TestLeaf(TestStep._0).setStep0("hello"),
        new TestLeaf(TestStep._0).setStep0("test"),
        new TestLeaf(TestStep._0).setStep0("world"));
  }

  Stream<TestLeaf> duplicate(Stream<TestLeaf> input) {
    return Stream.concat(input, input);
  }

  Stream<TestLeaf> keepOnlySingleCharacters(TestLeaf input) {
    if (input.getStep0().length() == 1) return Stream.of(input);

    return Stream.of();
  }

  Stream<TestLeaf> orderBy(Stream<TestLeaf> input) {
    return input.sorted(Comparator.comparing(TestLeaf::getStep0));
  }

  CompletableFuture<Stream<TestLeaf>> distinct(Stream<TestLeaf> input) {
    return CompletableFuture.supplyAsync(input::distinct);
  }
}
