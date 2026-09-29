package com.glm.glmback.feuilledetemps.domain;

import java.time.Instant;

public class EvaluationFutureException extends RuntimeException {

  public EvaluationFutureException(Instant evaluation) {
    super("L'instant d'evaluation " + evaluation + " depasse l'heure du serveur de plus de deux minutes.");
  }
}
