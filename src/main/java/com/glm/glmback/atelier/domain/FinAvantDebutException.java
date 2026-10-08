package com.glm.glmback.atelier.domain;

import java.time.Instant;

public final class FinAvantDebutException extends RuntimeException {

  public FinAvantDebutException(ActiviteId activite, Instant fin, Instant debut) {
    super("La fin du %s precede le debut de l'activite %s, le %s".formatted(fin, activite.uuid(), debut));
  }
}
