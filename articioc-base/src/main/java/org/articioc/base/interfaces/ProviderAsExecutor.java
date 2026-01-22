package org.articioc.base.interfaces;

import io.vavr.collection.Stream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.articioc.base.LeafCarrier;

public interface ProviderAsExecutor<A> extends Provider<A> {

  void setPipeline(Function<Stream<LeafCarrier<A>>, CompletableFuture<Stream<A>>> pipeline);
}
