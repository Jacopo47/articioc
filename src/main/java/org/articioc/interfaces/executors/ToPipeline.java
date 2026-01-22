package org.articioc.interfaces.executors;

import org.articioc.base.Leaf;
import org.articioc.base.interfaces.Provider;
import org.articioc.interfaces.Pipeline;

public interface ToPipeline<A extends Leaf<M>, M> {
  Pipeline<A> pipeline();

  Pipeline<A> pipeline(Provider<A> customProvider);
}
