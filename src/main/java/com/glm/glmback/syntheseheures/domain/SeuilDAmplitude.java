package com.glm.glmback.syntheseheures.domain;

/**
 * Le seuil d'amplitude de l'entreprise, lu a chaque releve : il decide quelles journees sans depart sont abandonnees.
 */
@FunctionalInterface
public interface SeuilDAmplitude {
  AmplitudeMaximale amplitudeMaximale();
}
