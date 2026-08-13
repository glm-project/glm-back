package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record Nom(String value) {
  private static final int MAX_LENGTH = 100;

  public Nom {
    Assert.field("nom", value).notBlank().maxLength(MAX_LENGTH);
  }
}
