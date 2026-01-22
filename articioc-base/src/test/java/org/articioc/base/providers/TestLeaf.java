package org.articioc.base.providers;

import org.articioc.base.SimpleLeaf;

public class TestLeaf extends SimpleLeaf {
  public TestLeaf(TestStep step) {
    super(step);
  }

  @Override
  public String key() {
    return getStep().getName();
  }
}
