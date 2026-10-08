package com.glm.glmback.categoriedeproduit.domain;

public final class CategorieUtiliseeException extends RuntimeException {

  public CategorieUtiliseeException(CodeDeCategorie code) {
    super("La categorie %s ne peut pas etre supprimee : des produits y sont ranges".formatted(code.value()));
  }
}
