package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

public record PosteDeTravailId(UUID uuid) {
  public PosteDeTravailId {
    Assert.notNull("id du poste de travail", uuid);
  }
}
