package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Ce que ce contexte retient d'un poste de travail : son identite, son libelle et sa nature.
 *
 * <p>
 * Rien n'en est copie sur l'operateur, qui ne stocke que l'identifiant : contrairement a l'atelier, aucun historique ne
 * pend a un poste, donc un poste renomme doit s'afficher renomme partout.
 * </p>
 */
public record PosteHabilitable(PosteHabilitableId id, LibelleDePoste libelle, NatureDeTravail nature) {
  public PosteHabilitable {
    Assert.notNull("id", id);
    Assert.notNull("libelle du poste", libelle);
    Assert.notNull("nature de travail", nature);
  }
}
