package com.glm.glmback.atelier.domain.correctiondesactivites;

/**
 * Ce que la lecture de la journee dit d'une activite a son instant d'evaluation.
 */
public enum EtatDActiviteDeLaJournee {
  /** Terminee par une fin reelle : pointee, regularisee, ajoutee ou posee par la cloture du suivi. */
  TERMINEE,
  /** Terminee a son echeance faute de fin reelle : c'est l'anomalie que le gestionnaire regularise. */
  TERMINEE_AUTOMATIQUEMENT,
  /** Rien ne l'a terminee et son echeance n'est pas atteinte : le pupitre la tient ouverte, elle n'a aucune duree. */
  EN_COURS,
}
