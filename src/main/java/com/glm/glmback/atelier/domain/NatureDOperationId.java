package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * L'identifiant d'une nature du referentiel des natures de travail, que ce contexte ne connait que par la donnee.
 */
public record NatureDOperationId(UUID uuid) {
  public NatureDOperationId {
    Assert.notNull("id de la nature de l'operation", uuid);
  }
}
