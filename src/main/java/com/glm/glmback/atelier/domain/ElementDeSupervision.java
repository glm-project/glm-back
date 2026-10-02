package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

public record ElementDeSupervision(ElementEngage element, Optional<String> reference) {
  public ElementDeSupervision {
    Assert.notNull("element", element);
    Assert.notNull("reference", reference);
  }
}
