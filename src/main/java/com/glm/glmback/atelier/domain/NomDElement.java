package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record NomDElement(String value) {
  private static final int MAX_LENGTH = 30;

  public NomDElement {
    Assert.field("nom de l'element", value).notBlank().maxLength(MAX_LENGTH);
  }
}
