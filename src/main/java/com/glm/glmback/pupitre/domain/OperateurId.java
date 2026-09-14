package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * L'identite de la personne qui pointe, telle que le referentiel des operateurs la designe.
 *
 * <p>
 * C'est la seule chose que le pupitre renvoie au serveur quand il pointe : le matricule sert a retrouver la fiche
 * localement, jamais a ecrire.
 * </p>
 */
public record OperateurId(UUID uuid) {
  public OperateurId {
    Assert.notNull("id de l'operateur", uuid);
  }
}
