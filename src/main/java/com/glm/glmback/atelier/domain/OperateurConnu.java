package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Ce que l'atelier retient d'un operateur du referentiel : son identite, relue a chaque lecture.
 *
 * <p>
 * Rien n'en est copie dans le journal, qui ne stocke que l'identifiant. C'est la difference exacte avec
 * {@link ElementEngage} : un element renomme ne doit pas reecrire l'histoire de l'atelier, alors qu'une identite
 * corrigee doit s'afficher corrigee partout.
 * </p>
 */
public record OperateurConnu(OperateurId id, Nom nom, Prenom prenom) {
  public OperateurConnu {
    Assert.notNull("id de l'operateur", id);
    Assert.notNull("nom", nom);
    Assert.notNull("prenom", prenom);
  }
}
