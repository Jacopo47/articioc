package org.articioc.base.interfaces;

import java.util.concurrent.CompletableFuture;
import org.articioc.base.models.CommitOperationOptions;

public interface CommitOperation<T> {
  CompletableFuture<T> commit(CommitOperationOptions<T> options);
}
