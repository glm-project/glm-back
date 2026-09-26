package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

public record CriteresDePointageEnAttente(Optional<OperateurId> operateur, Optional<MotifDeMiseEnAttente> motif) {
  public CriteresDePointageEnAttente {
    Assert.notNull("operateur", operateur);
    Assert.notNull("motif", motif);
  }

  public boolean matches(PointageEnAttente pointage) {
    return (
      !pointage.estTraite()
      && operateur.map(pointage.geste().operateur()::equals).orElse(true)
      && motif.map(pointage.motif()::equals).orElse(true)
    );
  }
}
