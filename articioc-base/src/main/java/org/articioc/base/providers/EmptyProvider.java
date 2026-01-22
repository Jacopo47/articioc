package org.articioc.base.providers;

import io.vavr.collection.Stream;
import org.articioc.base.LeafCarrier;
import org.articioc.base.interfaces.ProviderSync;

public class EmptyProvider<T> implements ProviderSync<T> {

  @Override
  public Stream<LeafCarrier<T>> readSync() {
    return Stream.empty();
  }

  @Override
  public T writeSync(T leaf) {
    return leaf;
  }

  @Override
  public Stream<T> writeSync(Stream<T> leaves) {
    return leaves;
  }

  @Override
  public void close() throws Exception {}
}
