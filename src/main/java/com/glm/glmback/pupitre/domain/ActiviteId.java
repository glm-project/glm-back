package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * L'identite d'une activite : celle de son pointage ouvrant d'origine.
 *
 * <p>
 * C'est elle que cible la fin d'une regularisation : le pupitre la recoit du referentiel et la renvoie telle quelle.
 * </p>
 */
public record ActiviteId(UUID uuid) {
  public ActiviteId {
    Assert.notNull("activite", uuid);
  }
}
