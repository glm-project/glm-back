package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * L'identite d'un element de fabrication, copiee dans l'atelier au moment de son engagement.
 *
 * <p>
 * Le nom et le type sont recopies plutot que relus : c'est ce qu'affiche l'ecran d'atelier, et un element renomme plus
 * tard ne doit pas reecrire l'histoire de l'atelier.
 * </p>
 */
public record ElementEngage(ElementEngageId id, NomDElement nom, TypeDElementEngage type) {
  public ElementEngage {
    Assert.notNull("id de l'element engage", id);
    Assert.notNull("nom de l'element", nom);
    Assert.notNull("type de l'element", type);
  }
}
