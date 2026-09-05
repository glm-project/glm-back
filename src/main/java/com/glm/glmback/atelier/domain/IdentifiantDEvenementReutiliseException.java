package com.glm.glmback.atelier.domain;

import java.util.UUID;

public class IdentifiantDEvenementReutiliseException extends RuntimeException {

  public IdentifiantDEvenementReutiliseException(UUID id) {
    super("L'identifiant d'evenement " + id + " est deja utilise.");
  }
}
