package org.articioc.base;

public abstract class Leaf<M> {
  private M metadata;
  private Step step;

  protected Leaf(M metadata, Step step) {
    this.metadata = metadata;
    this.step = step;
  }

  public M getMetadata() {
    return metadata;
  }

  public Leaf<M> setMetadata(M metadata) {
    this.metadata = metadata;
    return this;
  }

  public Step getStep() {
    return step;
  }

  public Leaf<M> setStep(Step step) {
    this.step = step;
    return this;
  }

  public abstract String key();
}
