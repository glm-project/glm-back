package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record NomDOperateur(String value) {
  private static final int MAX_LENGTH = 100;

  public NomDOperateur {
    Assert.field("nom de l'operateur", value).notBlank().maxLength(MAX_LENGTH);
  }
}
