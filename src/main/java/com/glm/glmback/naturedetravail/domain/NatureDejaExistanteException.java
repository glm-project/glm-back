package com.glm.glmback.naturedetravail.domain;

public final class NatureDejaExistanteException extends RuntimeException {

  public NatureDejaExistanteException(LibelleDeNature libelle) {
    super("Une nature de travail porte deja le libelle %s, a la casse ou aux accents pres".formatted(libelle.value()));
  }
}
