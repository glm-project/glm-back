package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record Prefixe(String value) {
  private static final int MAX_LENGTH = 10;

  public Prefixe {
    Assert.field("prefixe", value).notBlank().maxLength(MAX_LENGTH);
  }
}
