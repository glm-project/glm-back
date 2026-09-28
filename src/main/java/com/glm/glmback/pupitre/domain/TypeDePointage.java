package com.glm.glmback.pupitre.domain;

/**
 * Ce qu'un pointage dit d'une activite sur un element.
 *
 * <p>
 * Aucun type propre a la pause : le pupitre la pointe par une fin sur chaque activite en cours, et la reprise par un
 * debut ou une non conformite. Il n'a pas besoin de la presence pour afficher ses tuiles.
 * </p>
 */
public enum TypeDePointage {
  DEBUT,
  NON_CONFORMITE,
  FIN,
}
