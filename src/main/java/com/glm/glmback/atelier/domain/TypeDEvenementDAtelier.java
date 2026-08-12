package com.glm.glmback.atelier.domain;

/**
 * Ce qu'un pointage dit d'une activite sur un element.
 *
 * <p>
 * Ni pause ni reprise : elles appartiennent a la presence de l'operateur, pas a l'element. Une pause n'a jamais eu
 * besoin d'etre recopiee ici, le temps effectif se lisant a l'intersection des deux journaux.
 * </p>
 */
public enum TypeDEvenementDAtelier {
  DEBUT,
  NON_CONFORMITE,
  FIN,
}
