package com.glm.glmback.elementdefabrication.domain;

/**
 * Les categories de produit que l'entreprise a declarees, lues sans dependre de leur contexte.
 */
public interface CategoriesDeclarees {
  boolean existe(Categorie categorie);
}
