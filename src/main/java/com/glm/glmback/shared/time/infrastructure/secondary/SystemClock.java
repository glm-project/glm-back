package com.glm.glmback.shared.time.infrastructure.secondary;

import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class SystemClock implements Clock {

  private final java.time.Clock clock;

  SystemClock(java.time.Clock clock) {
    this.clock = clock;
  }

  @Override
  public Instant now() {
    return clock.instant();
  }
}
