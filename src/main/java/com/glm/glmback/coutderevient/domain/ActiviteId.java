package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/** Identite de l'ouverture originale, stable apres correction. */
public record ActiviteId(UUID uuid) {
  public ActiviteId {
    Assert.notNull("id de l'activite", uuid);
  }
}
