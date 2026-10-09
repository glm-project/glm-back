package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * L'identite d'une activite : celle du pointage qui l'a ouverte.
 *
 * <p>
 * C'est elle que cible la fin d'une regularisation, jamais l'identifiant technique de l'ouvrant. Les deux sont
 * aujourd'hui le meme identifiant.
 * </p>
 */
public record ActiviteId(UUID uuid) {
  public ActiviteId {
    Assert.notNull("id de l'activite", uuid);
  }

  public static ActiviteId ouvertePar(EvenementDAtelierId ouvrant) {
    return new ActiviteId(ouvrant.uuid());
  }
}
