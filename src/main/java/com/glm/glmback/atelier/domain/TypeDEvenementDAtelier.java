package com.glm.glmback.atelier.domain;

/**
 * Ce qu'un pointage dit d'une activite sur un element.
 *
 * <p>
 * Aucun type propre a la pause : le pupitre la pointe par une fin sur chaque activite en cours, et la reprise par un
 * debut, ou une non conformite pour l'activite qui en etait une.
 * </p>
 */
public enum TypeDEvenementDAtelier {
  DEBUT,
  NON_CONFORMITE,
  FIN,
}
