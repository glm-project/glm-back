package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Un poste de travail tel que le referentiel le nomme a l'instant de la lecture.
 */
public record PosteConnu(PosteDeTravailId id, LibelleDePoste libelle) {
  public PosteConnu {
    Assert.notNull("id du poste de travail", id);
    Assert.notNull("libelle du poste", libelle);
  }
}
