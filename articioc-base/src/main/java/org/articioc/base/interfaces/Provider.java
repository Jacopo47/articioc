package org.articioc.base.interfaces;

import io.vavr.collection.Stream;
import java.util.concurrent.CompletableFuture;
import org.articioc.base.LeafCarrier;

public interface Provider<T> extends AutoCloseable {
  CompletableFuture<Stream<LeafCarrier<T>>> read();

  CompletableFuture<T> write(T leaf);

  CompletableFuture<Stream<T>> write(Stream<T> leaves);
}
