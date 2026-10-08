package com.glm.glmback.categoriedeproduit.domain;

public final class CategorieDejaExistanteException extends RuntimeException {

  public CategorieDejaExistanteException(CodeDeCategorie code) {
    super("La categorie %s existe deja".formatted(code.value()));
  }
}
