package com.glm.glmback.elementdefabrication.domain;

public final class CategorieInconnueException extends RuntimeException {

  public CategorieInconnueException(Categorie categorie) {
    super("La categorie %s n'est pas declaree".formatted(categorie.value()));
  }
}
