package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record PrenomDOperateur(String value) {
  private static final int MAX_LENGTH = 100;

  public PrenomDOperateur {
    Assert.field("prenom de l'operateur", value).notBlank().maxLength(MAX_LENGTH);
  }
}
