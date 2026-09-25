package com.glm.glmback.atelier.domain;

/**
 * Deux journees d'un meme operateur ne se chevauchent jamais. Seul un acte du gestionnaire peut le provoquer, et il
 * est refuse : celui qui saisit peut corriger sur le champ.
 */
public final class ChevauchementDeJourneesException extends RuntimeException {

  public ChevauchementDeJourneesException(JourneeDeTravail journee, JourneeDeTravail autre) {
    super("La journee de travail %s chevaucherait la journee %s du meme operateur".formatted(journee.id().uuid(), autre.id().uuid()));
  }
}
