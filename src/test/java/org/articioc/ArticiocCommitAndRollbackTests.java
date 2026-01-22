package org.articioc;

import io.vavr.collection.Stream;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import org.articioc.base.LeafCarrier;
import org.articioc.base.interfaces.Provider;
import org.articioc.base.utils.Futures;
import org.articioc.interfaces.oneTo.OneToOne;
import org.articioc.tests.models.TestLeaf;
import org.articioc.tests.models.TestStep;
import org.articioc.tests.utils.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ArticiocCommitAndRollbackTests {

  class IncrementAtCommitOrRollbackProvider implements Provider<TestLeaf> {

    private int commitCounter;
    private int rollbackCounter;

    private final Queue<TestLeaf> queue;

    public IncrementAtCommitOrRollbackProvider() {
      this.commitCounter = 0;
      this.rollbackCounter = 0;
      this.queue = new LinkedBlockingQueue<>();
    }

    @Override
    public CompletableFuture<Stream<LeafCarrier<TestLeaf>>> read() {
      return Optional.ofNullable(queue.poll())
          .map(r -> LeafCarrier.from(r)
              .withCommit((options) -> {
                commitCounter++;
                return CompletableFuture.completedFuture(r);
              })
              .withRollback((options) -> {
                rollbackCounter++;
                return CompletableFuture.completedFuture(r);
              }))
          .map(Stream::of)
          .map(CompletableFuture::completedFuture)
          .orElseGet(() -> CompletableFuture.completedFuture(Stream.of()));
    }

    @Override
    public CompletableFuture<TestLeaf> write(TestLeaf leaf) {
      this.queue.add(leaf);
      return CompletableFuture.completedFuture(leaf);
    }

    @Override
    public CompletableFuture<Stream<TestLeaf>> write(Stream<TestLeaf> leaves) {
      return Futures.whenAllAsStream(leaves.map(this::write));
    }

    @Override
    public void close() {}

    public int getCommitCounter() {
      return commitCounter;
    }

    public int getRollbackCounter() {
      return rollbackCounter;
    }
  }

  @Test
  void basicScenario_everythingOk_shouldCommit() {
    var provider = new IncrementAtCommitOrRollbackProvider();

    var builder = new Articioc.Builder<>(
        provider,
        () -> CompletableFuture.completedFuture(Stream.of(new TestLeaf(TestStep._0))),
        TestStep._0);

    var articioc = Utils.basic(builder).end();

    /* Force records to be pushed on the provider… */
    articioc.trigger();

    while (articioc.readThenExecute().join().headOption().isDefined()) {
      /* read until there is something to process … */
    }

    Assertions.assertEquals(4, provider.getCommitCounter());
    Assertions.assertEquals(0, provider.getRollbackCounter());
  }

  @Test
  void basicScenario_errorOnLastPipeline_shouldCommitButTheLastOneShouldRollback() {
    var provider = new IncrementAtCommitOrRollbackProvider();

    var builder = new Articioc.Builder<>(
        provider,
        () -> CompletableFuture.completedFuture(Stream.of(new TestLeaf(TestStep._0))),
        TestStep._0);

    var articioc = Utils.basic(builder)
        .addStep((OneToOne<TestLeaf>) input -> {
          throw new RuntimeException("Unlucky exception.. :/ wasn't expected..");
        })
        .end();

    /* Force records to be pushed on the provider… */
    articioc.trigger();

    while (articioc.readThenExecute().join().headOption().isDefined()) {
      /* read until there is something to process … */
    }

    Assertions.assertEquals(3, provider.getCommitCounter());
    Assertions.assertEquals(1, provider.getRollbackCounter());
  }
}
