package com.glm.glmback.coutderevient.domain;

/**
 * Le seuil d'amplitude de l'entreprise, lu a chaque rapport : il decide quelles journees sans depart sont abandonnees.
 */
@FunctionalInterface
public interface SeuilDuCout {
  AmplitudeMaximale amplitudeMaximale();
}
