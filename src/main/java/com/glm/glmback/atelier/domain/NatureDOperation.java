package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * La nature d'une operation : l'identifiant de la nature du poste, recopie a la saisie, et son libelle courant, relu a
 * chaque lecture. Renommer la nature renomme donc celle de tous les pointages, sans en reecrire aucun : un ordre pointe
 * avant et apres le renommage garde une seule nature.
 */
public record NatureDOperation(NatureDOperationId id, String libelle) {
  private static final int MAX_LENGTH = 50;

  public NatureDOperation {
    Assert.notNull("id de la nature de l'operation", id);
    Assert.field("nature de l'operation", libelle).notBlank().maxLength(MAX_LENGTH);
  }
}
