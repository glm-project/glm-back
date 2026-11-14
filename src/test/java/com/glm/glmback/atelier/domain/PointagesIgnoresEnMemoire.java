package com.glm.glmback.atelier.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * L'audit en memoire : deux listes sans aucune contrainte, comme la table, une par forme de ligne.
 */
class PointagesIgnoresEnMemoire implements PointagesIgnores {

  private final List<PointageIgnore> lignes = new ArrayList<>();
  private final List<PointageEnAvance> enAvance = new ArrayList<>();

  @Override
  public void enregistre(PointageIgnore pointage) {
    lignes.add(pointage);
  }

  @Override
  public void enregistre(PointageEnAvance pointage) {
    enAvance.add(pointage);
  }

  @Override
  public boolean contient(EvenementDAtelierId pointage) {
    return (
      lignes.stream().anyMatch(ligne -> ligne.pointage().evenement().equals(pointage))
      || enAvance.stream().anyMatch(ligne -> ligne.pointage().evenement().equals(pointage))
    );
  }

  /**
   * Les pointages ignores par la regle de reception.
   */
  List<PointageIgnore> lignes() {
    return List.copyOf(lignes);
  }

  /**
   * Les pointages partis en audit pour une horloge en avance.
   */
  List<PointageEnAvance> enAvance() {
    return List.copyOf(enAvance);
  }
}
