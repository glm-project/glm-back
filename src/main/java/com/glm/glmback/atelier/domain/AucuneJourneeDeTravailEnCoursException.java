package com.glm.glmback.atelier.domain;

public final class AucuneJourneeDeTravailEnCoursException extends RuntimeException {

  public AucuneJourneeDeTravailEnCoursException(Operateur operateur) {
    super("L'operateur %s n'a aucune journee de travail en cours".formatted(operateur.value()));
  }
}
