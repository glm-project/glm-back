package com.glm.glmback.feuilledetemps.domain;

import java.time.Instant;
import java.util.List;

/** Projection en memoire : les valeurs sont deja interpretees, sans journal a replier. */
final class ActivitesEnMemoire implements ActivitesDeLOperateur {

  private final List<ActiviteInterpretee> activites;
  private Instant debutDemande;
  private Instant finExclusiveDemandee;

  private ActivitesEnMemoire(List<ActiviteInterpretee> activites) {
    this.activites = activites;
  }

  static ActivitesEnMemoire sansActivite() {
    return avec(List.of());
  }

  static ActivitesEnMemoire avec(List<ActiviteInterpretee> activites) {
    return new ActivitesEnMemoire(activites);
  }

  @Override
  public List<ActiviteInterpretee> recouvrant(OperateurId operateur, Instant debut, Instant finExclusive) {
    debutDemande = debut;
    finExclusiveDemandee = finExclusive;
    return activites;
  }

  Instant debutDemande() {
    return debutDemande;
  }

  Instant finExclusiveDemandee() {
    return finExclusiveDemandee;
  }
}
