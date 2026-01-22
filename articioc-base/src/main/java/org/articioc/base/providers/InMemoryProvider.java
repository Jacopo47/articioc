package org.articioc.base.providers;

import io.vavr.Tuple2;
import io.vavr.collection.Queue;
import io.vavr.collection.Stream;
import org.articioc.base.LeafCarrier;
import org.articioc.base.interfaces.ProviderSync;

public class InMemoryProvider<T> implements ProviderSync<T> {

  private Queue<T> queue;

  public InMemoryProvider() {
    this.queue = Queue.empty();
  }

  @Override
  public Stream<LeafCarrier<T>> readSync() {
    var result = queue.dequeueOption();
    queue = result.map(Tuple2::_2).getOrElse(Queue::empty);

    return result.map(Tuple2::_1).map(LeafCarrier::from).toStream();
  }

  @Override
  public T writeSync(T leaf) {
    synchronized (this) {
      queue = queue.enqueue(leaf);
    }

    return leaf;
  }

  @Override
  public Stream<T> writeSync(Stream<T> leaves) {
    for (var b : leaves) {
      writeSync(b);
    }

    return leaves;
  }

  @Override
  public void close() {}

  public Queue<T> getQueue() {
    return Queue.ofAll(this.queue);
  }
}
