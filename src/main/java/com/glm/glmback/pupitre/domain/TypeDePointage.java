package com.glm.glmback.pupitre.domain;

/**
 * Ce qu'un pointage dit d'une activite sur un element.
 *
 * <p>
 * Ni pause ni reprise : elles appartiennent a la presence de l'operateur, pas a l'element. Le pupitre n'a pas besoin
 * de la presence pour afficher ses tuiles.
 * </p>
 */
public enum TypeDePointage {
  DEBUT,
  NON_CONFORMITE,
  FIN,
}
