package com.glm.glmback.pupitre.domain;

/**
 * Le seuil d'amplitude de l'entreprise, lu a chaque generation du referentiel.
 */
@FunctionalInterface
public interface SeuilDuPupitre {
  AmplitudeMaximale amplitudeMaximale();
}
