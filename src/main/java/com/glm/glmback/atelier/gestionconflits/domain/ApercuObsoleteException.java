package com.glm.glmback.atelier.gestionconflits.domain;

public final class ApercuObsoleteException extends RuntimeException {

  public ApercuObsoleteException() {
    super("L'apercu doit etre renouvelle avant confirmation");
  }
}
