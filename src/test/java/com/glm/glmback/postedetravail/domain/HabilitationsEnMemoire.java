package com.glm.glmback.postedetravail.domain;

import java.util.HashSet;
import java.util.Set;

/**
 * Doublure de test du port d'habilitation : elle tient lieu du contexte des operateurs, que ce contexte ne connait que
 * par la donnee.
 */
final class HabilitationsEnMemoire implements PostesEnUsage {

  private final Set<PosteDeTravailId> habilites = new HashSet<>();

  void habilite(PosteDeTravailId poste) {
    habilites.add(poste);
  }

  void libere(PosteDeTravailId poste) {
    habilites.remove(poste);
  }

  @Override
  public boolean estHabilite(PosteDeTravailId poste) {
    return habilites.contains(poste);
  }
}
