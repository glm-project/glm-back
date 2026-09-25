package com.glm.glmback.atelier.domain;

/**
 * Ce que la liste des anomalies signale au gestionnaire.
 */
public enum TypeDAnomalie {
  /**
   * Une journee sans depart dont l'amplitude depasse le seuil : l'operateur est parti sans pointer.
   */
  JOURNEE_SANS_DEPART,

  /**
   * Une journee fermee dont l'amplitude depasse le seuil : une tres longue journee, ou un retour en soiree absorbe.
   */
  AMPLITUDE_EXCESSIVE,
}
