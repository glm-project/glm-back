package com.glm.glmback.atelier.domain;

/**
 * Une saisie a ete calculee sur un journal que quelqu'un d'autre a complete entre temps.
 *
 * <p>
 * Le journal est relu puis reecrit a chaque pointage. Deux saisies parties du meme etat ignorent chacune
 * le geste de l'autre : l'ecriture refuse donc un journal incomplet. L'application la rejoue sur les faits rafraichis,
 * sans perdre de fait.
 * </p>
 */
public final class SaisieConcurrenteException extends RuntimeException {

  public SaisieConcurrenteException(SuiviDAtelierId id) {
    super("Le suivi d'atelier %s a ete modifie par une autre saisie".formatted(id.uuid()));
  }
}
