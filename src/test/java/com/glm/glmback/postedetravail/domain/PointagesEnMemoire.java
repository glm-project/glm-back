package com.glm.glmback.postedetravail.domain;

import java.util.HashSet;
import java.util.Set;

/**
 * Doublure de test du port des pointages : elle tient lieu du contexte de l'atelier, que ce contexte ne connait que
 * par la donnee.
 */
final class PointagesEnMemoire implements PostesPointes {

  private final Set<PosteDeTravailId> pointes = new HashSet<>();

  void pointe(PosteDeTravailId poste) {
    pointes.add(poste);
  }

  @Override
  public boolean aServiAPointer(PosteDeTravailId poste) {
    return pointes.contains(poste);
  }
}
