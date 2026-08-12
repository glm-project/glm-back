package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record MotifDAnnulation(String value) {
  private static final int MAX_LENGTH = 255;

  public MotifDAnnulation {
    Assert.field("motif d'annulation", value).notBlank().maxLength(MAX_LENGTH);
  }
}
