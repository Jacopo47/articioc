package org.articioc.interfaces.executors;

import io.vavr.collection.Stream;
import java.util.concurrent.CompletableFuture;
import org.articioc.base.Leaf;
import org.articioc.base.interfaces.Provider;

public interface AutonomousExecutor<A extends Leaf<M>, M> {
  CompletableFuture<Stream<A>> readThenExecute();

  CompletableFuture<Stream<A>> trigger();

  CompletableFuture<Stream<A>> trigger(Provider<A> provider);
}
