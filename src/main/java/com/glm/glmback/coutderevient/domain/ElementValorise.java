package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * L'element de fabrication tel que le rapport le nomme, relu au referentiel a chaque lecture.
 *
 * <p>
 * Rien n'est fige ici, contrairement a l'atelier qui copie le nom a l'engagement : un element renomme doit s'afficher
 * renomme sur son rapport, qui parle de l'element et non de son passage en atelier.
 * </p>
 */
public record ElementValorise(ElementId element, NomDElement nom, TypeDElement type) {
  public ElementValorise {
    Assert.notNull("element", element);
    Assert.notNull("nom de l'element", nom);
    Assert.notNull("type de l'element", type);
  }
}
