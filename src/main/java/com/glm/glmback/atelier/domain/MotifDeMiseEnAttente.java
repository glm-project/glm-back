package com.glm.glmback.atelier.domain;

/**
 * Pourquoi un geste du pupitre n'a pu etre rattache a rien (lot 8c de la strategie « bornes de fin de journee »).
 */
public enum MotifDeMiseEnAttente {
  /** Operateur inconnu du referentiel : fiche supprimee, ou referentiel du pupitre perime. */
  OPERATEUR_INCONNU,
  /** Poste de travail inconnu du referentiel. */
  POSTE_INCONNU,
  /** Element de fabrication jamais engage, ou inconnu du serveur. */
  ELEMENT_INCONNU,
  /** Geste rejoue dans le desordre qui casse l'enchainement, ou date dans une journee deja fermee. */
  GESTE_HORS_SEQUENCE,
  /** Identifiant de geste deja pris par un autre contenu : defaut du pupitre. */
  IDENTIFIANT_REUTILISE,
}
