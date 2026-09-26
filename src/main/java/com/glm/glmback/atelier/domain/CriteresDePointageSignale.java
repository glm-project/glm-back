package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Regles de selection des pointages signales : seuls ceux qui ne sont pas resolus, et deux filtres facultatifs.
 */
public record CriteresDePointageSignale(Optional<OperateurId> operateur, Optional<MotifDeSignalement> motif) {
  public CriteresDePointageSignale {
    Assert.notNull("operateur", operateur);
    Assert.notNull("motif", motif);
  }

  public boolean matches(PointageSignale pointage) {
    return (
      !pointage.estResolu()
      && operateur.map(pointage.operateur()::equals).orElse(true)
      && motif.map(pointage.motifs()::contains).orElse(true)
    );
  }
}
