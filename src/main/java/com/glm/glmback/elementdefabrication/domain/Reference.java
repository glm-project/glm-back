package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public record Reference(String value) {
  private static final int MAX_LENGTH = 100;

  public Reference {
    Assert.field("reference", value).notBlank().maxLength(MAX_LENGTH);
  }

  public static Optional<Reference> of(String value) {
    return Optional.ofNullable(value).filter(StringUtils::isNotBlank).map(Reference::new);
  }
}
