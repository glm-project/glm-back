package com.glm.glmback.atelier.domain;

/**
 * Une regularisation de fin sur une activite qui n'est pas une fin automatique : elle n'a pas atteint son echeance, ou
 * un pointage l'a deja terminee.
 */
public final class ActiviteNonEchueException extends RuntimeException {

  public ActiviteNonEchueException(ActiviteId activite) {
    super("L'activite %s n'est pas echue : sa fin ne se regularise pas".formatted(activite.uuid()));
  }
}
