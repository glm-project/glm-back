package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Set;

/** Les etats demandes, et les conflits qui doivent rester visibles independamment de l'etat. */
public record SelectionDeSuivis(Set<EtatDAtelier> etats, boolean inclureConflits) {
  public SelectionDeSuivis {
    Assert.field("etats", etats).notNull().noNullElement();
    etats = Set.copyOf(etats);
  }

  public boolean matches(SuiviDAtelier suivi, Instant evaluation) {
    return etats.isEmpty() || etats.contains(suivi.etat(evaluation)) || (inclureConflits && !suivi.conflits().isEmpty());
  }
}
