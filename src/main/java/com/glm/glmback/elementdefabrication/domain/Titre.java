package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record Titre(String value) {
  private static final int MAX_LENGTH = 255;

  public Titre {
    Assert.field("titre", value).notBlank().maxLength(MAX_LENGTH);
  }
}
