package com.glm.glmback.atelier.gestionconflits.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

/** Le debut metier et le nombre de faits d'une sequence projetee. */
public record RepereDeSequence(Instant premierPointage, int nombrePointages) {
  public RepereDeSequence {
    Assert.notNull("premier pointage", premierPointage);
    Assert.field("nombre de pointages", nombrePointages).min(1);
  }
}
