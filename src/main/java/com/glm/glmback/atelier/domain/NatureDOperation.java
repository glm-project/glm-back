package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record NatureDOperation(String value) {
  private static final int MAX_LENGTH = 50;

  public NatureDOperation {
    Assert.field("nature de l'operation", value).notBlank().maxLength(MAX_LENGTH);
  }
}
