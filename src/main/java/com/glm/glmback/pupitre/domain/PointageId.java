package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/** Identite d'un fait conserve dans une sequence en conflit. */
public record PointageId(UUID uuid) {
  public PointageId {
    Assert.notNull("pointage", uuid);
  }
}
