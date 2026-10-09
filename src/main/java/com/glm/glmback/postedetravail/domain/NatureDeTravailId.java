package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * L'identifiant d'une nature du referentiel des natures de travail, que ce contexte ne connait que par la donnee.
 */
public record NatureDeTravailId(UUID uuid) {
  public NatureDeTravailId {
    Assert.notNull("id de la nature de travail", uuid);
  }

  public static NatureDeTravailId newId() {
    return new NatureDeTravailId(UUID.randomUUID());
  }
}
