package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * La categorie de l'element, copiee a l'engagement par l'atelier.
 *
 * <p>
 * Une valeur libre et non une liste fermee : chaque entreprise nomme ses propres categories de produit, et ce contexte
 * n'a pas a les connaitre pour les restituer.
 * </p>
 */
public record CategorieDElement(String value) {
  public CategorieDElement {
    Assert.notBlank("categorie de l'element", value);
  }
}
