package com.glm.glmback.atelier.domain;

public final class PointageIgnoreException extends RuntimeException {

  public PointageIgnoreException(EvenementDAtelierId pointage) {
    super("Le pointage %s est ignore par la regle de reception".formatted(pointage.uuid()));
  }
}
