package org.articioc.base.interfaces;

import io.vavr.collection.Stream;
import java.util.concurrent.CompletableFuture;
import org.articioc.base.LeafCarrier;

public interface ProviderSync<T> extends Provider<T> {
  Stream<LeafCarrier<T>> readSync();

  T writeSync(T leaf);

  Stream<T> writeSync(Stream<T> leaves);

  @Override
  default CompletableFuture<Stream<LeafCarrier<T>>> read() {
    return CompletableFuture.completedFuture(readSync());
  }

  @Override
  default CompletableFuture<T> write(T leaf) {
    return CompletableFuture.completedFuture(writeSync(leaf));
  }

  @Override
  default CompletableFuture<Stream<T>> write(Stream<T> leaves) {
    return CompletableFuture.completedFuture(writeSync(leaves));
  }
}
