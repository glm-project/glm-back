package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * L'element d'un suivi, avec le nom et la categorie que l'atelier a copies a l'engagement : un element reengage apres
 * cloture en porte les memes.
 */
public record ElementEngage(ElementId id, NomDElement nom, CategorieDElement categorie) {
  public ElementEngage {
    Assert.notNull("id de l'element de fabrication", id);
    Assert.notNull("nom de l'element", nom);
    Assert.notNull("categorie de l'element", categorie);
  }
}
