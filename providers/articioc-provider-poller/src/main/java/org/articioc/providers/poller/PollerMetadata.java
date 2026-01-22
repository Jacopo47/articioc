package org.articioc.providers.poller;

import org.articioc.base.interfaces.leaf.WithNotBefore;

public interface PollerMetadata extends WithNotBefore {
  int getTentative();

  PollerMetadata setTentative(int tentative);

  PollerRecordStatus getStatus();

  PollerMetadata setStatus(PollerRecordStatus status);
}
