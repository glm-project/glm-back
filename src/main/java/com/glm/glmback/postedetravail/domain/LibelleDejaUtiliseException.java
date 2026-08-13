package com.glm.glmback.postedetravail.domain;

public final class LibelleDejaUtiliseException extends RuntimeException {

  public LibelleDejaUtiliseException(Libelle libelle) {
    super("Le libelle %s est deja utilise par un autre poste de travail".formatted(libelle.value()));
  }
}
