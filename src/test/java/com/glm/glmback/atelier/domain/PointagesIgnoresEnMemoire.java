package com.glm.glmback.atelier.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * L'audit en memoire : une liste sans aucune contrainte, comme la table.
 */
class PointagesIgnoresEnMemoire implements PointagesIgnores {

  private final List<PointageIgnore> lignes = new ArrayList<>();

  @Override
  public void enregistre(PointageIgnore pointage) {
    lignes.add(pointage);
  }

  @Override
  public boolean contient(EvenementDAtelierId pointage) {
    return lignes.stream().anyMatch(ligne -> ligne.pointage().evenement().equals(pointage));
  }

  List<PointageIgnore> lignes() {
    return List.copyOf(lignes);
  }
}
