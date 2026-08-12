package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record Auteur(String value) {
  private static final int MAX_LENGTH = 100;

  public Auteur {
    Assert.field("auteur", value).notBlank().maxLength(MAX_LENGTH);
  }
}
