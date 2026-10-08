package com.glm.glmback.categoriedeproduit.domain;

public final class CategorieIntrouvableException extends RuntimeException {

  public CategorieIntrouvableException(CodeDeCategorie code) {
    super("La categorie %s est introuvable".formatted(code.value()));
  }
}
