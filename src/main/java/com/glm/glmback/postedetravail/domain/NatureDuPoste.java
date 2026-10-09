package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * La nature que porte un poste : son identifiant dans le referentiel, seul persiste sur le poste, et son libelle
 * courant, relu a chaque lecture. Renommer la nature renomme donc celle de tous ses postes.
 */
public record NatureDuPoste(NatureDeTravailId id, NatureDeTravail libelle) {
  public NatureDuPoste {
    Assert.notNull("id de la nature de travail", id);
    Assert.notNull("nature de travail", libelle);
  }
}
