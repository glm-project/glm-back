package com.glm.glmback.operateur.domain;

import java.util.HashSet;
import java.util.Set;

/**
 * Doublure de test du port des pointages : elle tient lieu du contexte de l'atelier, que ce contexte ne connait que
 * par la donnee.
 */
final class PointagesEnMemoire implements OperateursQuiOntPointe {

  private final Set<OperateurId> ontPointe = new HashSet<>();

  void pointe(OperateurId operateur) {
    ontPointe.add(operateur);
  }

  @Override
  public boolean aPointe(OperateurId operateur) {
    return ontPointe.contains(operateur);
  }
}
