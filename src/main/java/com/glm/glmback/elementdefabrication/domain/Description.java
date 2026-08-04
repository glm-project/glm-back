package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record Description(String value) {
  private static final int MAX_LENGTH = 1000;

  public Description {
    Assert.field("description", value).notBlank().maxLength(MAX_LENGTH);
  }
}
