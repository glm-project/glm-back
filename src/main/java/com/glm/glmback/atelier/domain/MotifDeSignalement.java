package com.glm.glmback.atelier.domain;

/**
 * Pourquoi un pointage enregistre malgre tout merite le regard du gestionnaire.
 */
public enum MotifDeSignalement {
  /**
   * Le poste pointe n'est pas, ou plus, dans les habilitations de l'operateur : le temps compte, le gestionnaire peut
   * l'annuler.
   */
  OPERATEUR_NON_HABILITE,

  /**
   * Le geste etait date avant l'engagement de l'OF, horloge du pupitre en retard : il a ete ramene a l'engagement.
   */
  DATE_ANTERIEURE_A_L_ENGAGEMENT,

  /**
   * Le geste etait date dans le futur, horloge du pupitre en avance : il a ete ramene a sa reception.
   */
  DATE_FUTURE,
}
