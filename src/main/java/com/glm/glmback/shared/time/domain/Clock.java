package com.glm.glmback.shared.time.domain;

import java.time.Instant;

@FunctionalInterface
public interface Clock {
  Instant now();
}
