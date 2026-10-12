package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record AuteurDeLaCloture(String value) {
  public AuteurDeLaCloture {
    Assert.notBlank("auteur de la cloture", value);
  }
}
