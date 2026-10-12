package com.glm.glmback.syntheseheures.domain;

public class PeriodeFutureException extends RuntimeException {

  public PeriodeFutureException(PeriodeCalendaire periode) {
    super("La periode du " + periode.premierJour() + " au " + periode.dernierJour() + " n'a pas encore commence.");
  }
}
