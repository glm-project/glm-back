package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * L'element d'un suivi, avec le nom et le type que l'atelier a copies a l'engagement : un element reengage apres
 * cloture en porte les memes.
 */
public record ElementEngage(ElementId id, NomDElement nom, TypeDElement type) {
  public ElementEngage {
    Assert.notNull("id de l'element de fabrication", id);
    Assert.notNull("nom de l'element", nom);
    Assert.notNull("type de l'element", type);
  }
}
