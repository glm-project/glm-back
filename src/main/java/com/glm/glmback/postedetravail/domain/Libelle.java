package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record Libelle(String value) {
  private static final int MAX_LENGTH = 100;

  public Libelle {
    Assert.field("libelle", value).notBlank().maxLength(MAX_LENGTH);
  }
}
