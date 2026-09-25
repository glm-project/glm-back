package com.glm.glmback.atelier.domain;

/**
 * Le seuil d'amplitude de l'entreprise courante, lu a chaque traitement : un changement vaut pour les gestes qui
 * suivent.
 */
@FunctionalInterface
public interface SeuilDAmplitude {
  AmplitudeMaximale amplitudeMaximale();
}
