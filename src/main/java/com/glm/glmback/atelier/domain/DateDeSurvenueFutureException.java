package com.glm.glmback.atelier.domain;

import java.time.Instant;

public class DateDeSurvenueFutureException extends RuntimeException {

  public DateDeSurvenueFutureException(Instant dateDeSurvenue) {
    super("La date de survenue " + dateDeSurvenue + " ne peut pas etre future.");
  }
}
