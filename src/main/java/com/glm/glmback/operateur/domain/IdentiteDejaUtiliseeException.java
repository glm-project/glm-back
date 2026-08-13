package com.glm.glmback.operateur.domain;

public final class IdentiteDejaUtiliseeException extends RuntimeException {

  public IdentiteDejaUtiliseeException(Nom nom, Prenom prenom) {
    super("Un autre operateur se nomme deja %s %s".formatted(prenom.value(), nom.value()));
  }
}
