package com.glm.glmback.atelier.domain;

import java.time.Instant;

public final class FinApresBorneException extends RuntimeException {

  public FinApresBorneException(ActiviteId activite, Instant fin, Instant borne) {
    super("La fin du %s de l'activite %s depasse sa borne, le %s".formatted(fin, activite.uuid(), borne));
  }
}
