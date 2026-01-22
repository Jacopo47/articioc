package org.articioc.base;

import io.vavr.collection.Stream;
import java.util.Optional;
import org.articioc.base.interfaces.CommitOperation;
import org.articioc.base.interfaces.RollbackOperation;

public class LeafCarrier<T> {
  private final T data;
  private CommitOperation<T> CommitOperation;
  private RollbackOperation<T> RollbackOperation;

  public LeafCarrier(T data) {
    this.data = data;
  }

  public static <T> LeafCarrier<T> from(T record) {
    return new LeafCarrier<>(record);
  }

  public static <T> Stream<LeafCarrier<T>> from(Stream<T> record) {
    return record.map(LeafCarrier::from);
  }

  public LeafCarrier<T> withCommit(CommitOperation<T> commitOperation) {
    this.CommitOperation = commitOperation;
    return this;
  }

  public LeafCarrier<T> withRollback(RollbackOperation<T> rollbackOperation) {
    this.RollbackOperation = rollbackOperation;
    return this;
  }

  public T getData() {
    return data;
  }

  public Optional<CommitOperation<T>> getCommitOperation() {
    return Optional.ofNullable(CommitOperation);
  }

  public Optional<RollbackOperation<T>> getRollbackOperation() {
    return Optional.ofNullable(RollbackOperation);
  }
}
