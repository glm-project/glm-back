package com.glm.glmback.atelier.domain;

public final class ApercuObsoleteException extends RuntimeException {

  public ApercuObsoleteException() {
    super("L'apercu doit etre renouvelle avant confirmation");
  }
}
