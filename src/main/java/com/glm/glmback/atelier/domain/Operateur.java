package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record Operateur(String value) {
  private static final int MAX_LENGTH = 100;

  public Operateur {
    Assert.field("operateur", value).notBlank().maxLength(MAX_LENGTH);
  }
}
