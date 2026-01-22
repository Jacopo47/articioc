package org.articioc.base.interfaces.leaf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

public interface WithNotBefore {
  ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

  LocalDateTime getNotBefore();

  WithNotBefore setNotBefore(LocalDateTime notBefore);

  default WithNotBefore withDelay(Duration delay) {
    var now = LocalDateTime.now(getZoneId());

    return setNotBefore(now.plus(delay));
  }

  @JsonIgnore
  default ZoneId getZoneId() {
    return DEFAULT_ZONE_ID;
  }
}
