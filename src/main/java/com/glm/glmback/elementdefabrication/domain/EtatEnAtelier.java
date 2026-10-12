package com.glm.glmback.elementdefabrication.domain;

/**
 * L'etat d'un element dont le suivi est ouvert, juge par l'atelier a l'instant de la lecture.
 */
public enum EtatEnAtelier {
  /** Rien n'a encore ete pointe. */
  EN_ATTENTE,
  /** Une activite court, son echeance n'est pas atteinte. */
  EN_COURS,
  /** Des pointages existent, mais plus aucune activite ne court. */
  INTERROMPU,
}
