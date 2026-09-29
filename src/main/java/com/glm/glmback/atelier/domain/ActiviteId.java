package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * L'identite d'une activite : celle du pointage qui l'a ouverte.
 *
 * <p>
 * C'est elle que visent la fin et la transition, jamais l'identifiant technique de l'ouvrant actif : le remplacant
 * d'une correction reprend l'identite de l'activite qu'ouvrait le fait corrige, et les gestes qui la visent y restent
 * rattaches.
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
