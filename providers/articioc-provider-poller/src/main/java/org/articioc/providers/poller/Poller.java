package org.articioc.providers.poller;

import io.vavr.collection.Stream;
import java.util.concurrent.CompletableFuture;
import org.articioc.base.Leaf;
import org.articioc.base.LeafCarrier;
import org.articioc.base.interfaces.CommitOperation;
import org.articioc.base.interfaces.Provider;
import org.articioc.base.interfaces.RollbackOperation;
import org.articioc.base.utils.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Poller<A extends Leaf<M>, M extends PollerMetadata> extends Provider<A> {

  Logger logger = LoggerFactory.getLogger(Poller.class);

  @Override
  default CompletableFuture<Stream<LeafCarrier<A>>> read() {
    return identifyRecords()
        .thenCompose(this::executeTakeOnRecords)
        .thenApply(LeafCarrier::from)
        .thenApply(record -> record.map(r -> r.withCommit(commitOperation(r.getData()))
            .withRollback(rollbackOperation(r.getData()))));
  }

  default CommitOperation<A> commitOperation(A input) {
    return (options) -> {

      /*  The default scenario identifies only two reason for marking the record as Done:
       *       - when the last step of the pipeline is performed so no other steps are expected;
       *      - when it's not the last step but the current one has produced 0 records so no other steps must be executed;
       *   In  all the other scenario the pipeline will commit the record into the database, and it will handle the status by it own. */
      if (input.getStep().isFinal() || options.recordsInOutput().isEmpty()) {
        input.getMetadata().setStatus(PollerRecordStatus.Done);

        return write(input);
      }

      return CompletableFuture.completedFuture(input);
    };
  }

  default RollbackOperation<A> rollbackOperation(A input) {
    return (options) -> {
      input
          .getMetadata()
          .setTentative(input.getMetadata().getTentative() + 1)
          .setStatus(PollerRecordStatus.Ready);

      return write(input);
    };
  }

  private CompletableFuture<Stream<A>> executeTakeOnRecords(Stream<A> input) {
    var executeUpdateOnAllTheRecords = input
        .map(record -> take(record)
            .thenApply(Stream::of)
            .exceptionallyCompose(ex -> {
              if (logger.isWarnEnabled()) {
                logger.warn(
                    "Error while evaluating record with keys: {} at steps: {}.",
                    record.key(),
                    record.getStep(),
                    ex);
              }

              return CompletableFuture.completedFuture(Stream.empty());
            }));

    return Futures.whenAll(executeUpdateOnAllTheRecords);
  }

  CompletableFuture<A> take(A input);

  CompletableFuture<Stream<A>> identifyRecords();

  String getId();
}
