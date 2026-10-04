package com.glm.glmback.atelier.domain.gestionconflits;

public final class ApercuInvalideException extends RuntimeException {

  public ApercuInvalideException() {
    super("L'apercu ne peut pas etre utilise pour cette confirmation");
  }
}
