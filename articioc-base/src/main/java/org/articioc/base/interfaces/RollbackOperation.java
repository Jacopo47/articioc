package org.articioc.base.interfaces;

import java.util.concurrent.CompletableFuture;
import org.articioc.base.models.RollbackOperationOptions;

public interface RollbackOperation<T> {
  CompletableFuture<T> rollback(RollbackOperationOptions<T> options);
}
