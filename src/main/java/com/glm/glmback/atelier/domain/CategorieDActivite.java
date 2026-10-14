package com.glm.glmback.atelier.domain;

public enum CategorieDActivite {
  TRAVAIL,
  NON_CONFORMITE;

  /**
   * Le type du fait qui ouvre une activite de cette categorie : l'inverse de {@link TypeDEvenementDAtelier#categorie()}.
   */
  public TypeDEvenementDAtelier typeDOuverture() {
    return this == TRAVAIL ? TypeDEvenementDAtelier.DEBUT : TypeDEvenementDAtelier.NON_CONFORMITE;
  }
}
