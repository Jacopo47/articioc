package org.articioc.tests.utils;

import io.vavr.collection.Stream;
import io.vavr.control.Either;
import java.util.concurrent.CompletableFuture;
import org.articioc.Articioc;
import org.articioc.tests.models.TestLeaf;
import org.articioc.tests.models.TestStep;

public class Utils {

  public static Articioc.Builder<TestLeaf, TestStep> basic(
      Articioc.Builder<TestLeaf, TestStep> builder) {
    return builder
        .addStep(Utils::oneToOne)
        .addStep(Utils::oneToOneOnStep1)
        .checkpoint(TestStep._1)
        .addStep(Utils::enrichStep1)
        .checkpoint(TestStep._2)
        .addStep(Utils::enrichStep2)
        .addStep(Utils::enrichStep3)
        .checkpoint(TestStep._3)
        .addStep(Utils::duplicate)
        .addStep(Utils::enrichStep4)
        .addStep(Utils::enrichStep4);
  }

  public static TestLeaf oneToOne(TestLeaf input) {
    return input.setStep0("OneToOne step");
  }

  public static TestLeaf oneToOneOnStep1(TestLeaf input) {
    return input.setStep1("OneToOne step");
  }

  public static Stream<TestLeaf> duplicate(TestLeaf input) {
    return Stream.of(input.copy(), input.copy());
  }

  public static TestLeaf enrichStep1(TestLeaf input) {
    return input.setStep1(input.getStep1() + ", enriched!");
  }

  public static Either<Exception, TestLeaf> enrichStep2(TestLeaf input) {
    return Either.right(input.setStep2("Step 2 here!"));
  }

  public static CompletableFuture<Either<Exception, TestLeaf>> enrichStep3(TestLeaf input) {
    return CompletableFuture.completedFuture(Either.right(input.setStep3("Step 3 here!")));
  }

  public static CompletableFuture<TestLeaf> enrichStep4(TestLeaf input) {
    return CompletableFuture.completedFuture(input.setStep4("Step 4 here!"));
  }
}
