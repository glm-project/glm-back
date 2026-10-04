package com.glm.glmback.atelier.domain.gestionconflits;

public class PropositionInvalideException extends RuntimeException {

  public PropositionInvalideException() {
    super("proposition invalide");
  }
}
