package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/** Les bornes factuelles projetees par atelier, sans relecture concurrente du journal. */
public record ActiviteInterpretee(Activite activite, Plage plage, Instant echeance) {
  public ActiviteInterpretee {
    Assert.notNull("activite", activite);
    Assert.notNull("plage", plage);
    Assert.notNull("echeance", echeance);
  }

  public Optional<TrancheDActivite> termineeA(Instant evaluation) {
    return plage
      .fin()
      .or(() -> evaluation.isBefore(echeance) ? Optional.empty() : Optional.of(echeance))
      .map(fin -> new TrancheDActivite(activite, new Periode(plage.debut(), fin), plage.fin().isEmpty()));
  }
}
