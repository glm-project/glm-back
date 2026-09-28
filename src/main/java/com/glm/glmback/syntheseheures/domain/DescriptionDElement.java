package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record DescriptionDElement(String value) {
  private static final int MAX_LENGTH = 1000;

  public DescriptionDElement {
    Assert.field("description", value).notBlank().maxLength(MAX_LENGTH);
  }
}
