package org.articioc.tests.scenarios;

import org.articioc.base.interfaces.Provider;

public interface BasicOperationsProviderIntegrationTest<B extends BasicProviderTestLeaf<M>, M> {
  Provider<B> provider();

  BasicProviderTestLeaf<M> first();
}
