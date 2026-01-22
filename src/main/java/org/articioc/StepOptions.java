package org.articioc;

import java.util.Optional;
import org.articioc.base.Leaf;
import org.articioc.base.Step;
import org.articioc.base.interfaces.Provider;

public class StepOptions<A extends Leaf<M>, M> {

  private EndlessOptions<A, M> endless;
  private CheckpointOptions<A, M> checkpoint;

  public StepOptions() {}

  public Optional<EndlessOptions<A, M>> getEndless() {
    return Optional.ofNullable(endless);
  }

  public StepOptions<A, M> setEndless(EndlessOptions<A, M> endless) {
    this.endless = endless;
    return this;
  }

  public Optional<CheckpointOptions<A, M>> getCheckpoint() {
    return Optional.ofNullable(checkpoint);
  }

  public StepOptions<A, M> setCheckpoint(CheckpointOptions<A, M> checkpoint) {
    this.checkpoint = checkpoint;
    return this;
  }

  public record EndlessOptions<A extends Leaf<M>, M>(
      Provider<A> provider, Step step, Boolean shouldCollect) {}

  public record CheckpointOptions<A extends Leaf<M>, M>(boolean readonly) {}
}
