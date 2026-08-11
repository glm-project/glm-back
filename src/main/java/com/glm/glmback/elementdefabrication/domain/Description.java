package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public record Description(String value) {
  private static final int MAX_LENGTH = 1000;

  public Description {
    Assert.field("description", value).notBlank().maxLength(MAX_LENGTH);
  }

  public static Optional<Description> of(String value) {
    return Optional.ofNullable(value).filter(StringUtils::isNotBlank).map(Description::new);
  }
}
