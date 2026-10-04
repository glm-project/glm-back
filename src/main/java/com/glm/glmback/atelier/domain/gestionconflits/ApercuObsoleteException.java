package com.glm.glmback.atelier.domain.gestionconflits;

public final class ApercuObsoleteException extends RuntimeException {

  public ApercuObsoleteException() {
    super("L'apercu doit etre renouvelle avant confirmation");
  }
}
