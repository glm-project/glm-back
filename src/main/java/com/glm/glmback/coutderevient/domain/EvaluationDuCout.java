package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

/** Instant commun de la lecture et nombre d'activites exclues. */
public record EvaluationDuCout(Instant evaluation, int activitesEnCours) {
  public EvaluationDuCout {
    Assert.notNull("evaluation", evaluation);
    Assert.field("activites en cours", activitesEnCours).min(0);
  }
}
