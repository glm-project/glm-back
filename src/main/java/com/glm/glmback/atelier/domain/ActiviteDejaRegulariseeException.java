package com.glm.glmback.atelier.domain;

/**
 * La fin de cette activite a deja ete regularisee par le gestionnaire : une seule regularisation par activite.
 */
public final class ActiviteDejaRegulariseeException extends RuntimeException {

  public ActiviteDejaRegulariseeException(ActiviteId activite) {
    super("La fin de l'activite %s est deja regularisee".formatted(activite.uuid()));
  }
}
