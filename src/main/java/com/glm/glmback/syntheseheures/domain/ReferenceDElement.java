package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record ReferenceDElement(String value) {
  private static final int MAX_LENGTH = 100;

  public ReferenceDElement {
    Assert.field("reference", value).notBlank().maxLength(MAX_LENGTH);
  }
}
