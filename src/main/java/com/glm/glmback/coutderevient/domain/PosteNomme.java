package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;

/** Le poste de travail tel que le detail d'un pointage le nomme, relu au referentiel a chaque lecture. */
public record PosteNomme(PosteDeTravailId poste, LibelleDePoste libelle) {
  public PosteNomme {
    Assert.notNull("poste de travail", poste);
    Assert.notNull("libelle du poste", libelle);
  }
}
