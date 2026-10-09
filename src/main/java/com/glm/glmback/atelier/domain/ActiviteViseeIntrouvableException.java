package com.glm.glmback.atelier.domain;

/**
 * Une regularisation qui cible une activite qu'aucun pointage de ce suivi n'a jamais ouverte. Le refus est definitif :
 * rejouer la meme saisie ne la fera pas apparaitre.
 */
public final class ActiviteViseeIntrouvableException extends RuntimeException {

  public ActiviteViseeIntrouvableException(ActiviteId visee) {
    super("L'activite %s est introuvable dans ce suivi".formatted(visee.uuid()));
  }
}
