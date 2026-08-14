package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record Prenom(String value) {
  private static final int MAX_LENGTH = 100;

  public Prenom {
    Assert.field("prenom", value).notBlank().maxLength(MAX_LENGTH);
  }
}
