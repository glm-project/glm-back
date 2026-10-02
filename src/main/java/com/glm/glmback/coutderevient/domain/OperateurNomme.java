package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * L'operateur tel que le detail d'un pointage le nomme, relu au referentiel a chaque lecture : un operateur renomme
 * s'affiche renomme, y compris sur ses heures anciennes.
 */
public record OperateurNomme(OperateurId operateur, PrenomDOperateur prenom, NomDOperateur nom) {
  public OperateurNomme {
    Assert.notNull("operateur", operateur);
    Assert.notNull("prenom de l'operateur", prenom);
    Assert.notNull("nom de l'operateur", nom);
  }
}
