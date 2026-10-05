package com.glm.glmback.atelier.domain.gestionanomalies;

public final class ApercuObsoleteException extends RuntimeException {

  public ApercuObsoleteException() {
    super("L'apercu doit etre renouvelle avant confirmation");
  }
}
