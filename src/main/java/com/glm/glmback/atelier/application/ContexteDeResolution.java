package com.glm.glmback.atelier.application;

import com.glm.glmback.shared.error.domain.Assert;

public record ContexteDeResolution(String tenant, IdentiteDuGestionnaire gestionnaire) {
  public ContexteDeResolution {
    Assert.notBlank("tenant", tenant);
    Assert.notNull("gestionnaire", gestionnaire);
  }
}
