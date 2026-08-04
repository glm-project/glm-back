package com.glm.glmback.shared.time.domain;

import java.time.Instant;

public final class TimeFixture {

  private TimeFixture() {}

  public static Clock fixedClock(Instant instant) {
    return () -> instant;
  }
}
