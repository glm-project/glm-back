package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * L'identite de la personne dont la synthese des heures est lue, telle que le referentiel des operateurs la
 * designe.
 */
public record OperateurId(UUID uuid) {
  public OperateurId {
    Assert.notNull("id de l'operateur", uuid);
  }
}
