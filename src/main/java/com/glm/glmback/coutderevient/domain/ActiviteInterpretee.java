package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/** Les bornes factuelles projetees par atelier, sans relecture concurrente du journal. */
public record ActiviteInterpretee(ActiviteId id, Activite activite, Plage plage, Instant echeance) {
  public ActiviteInterpretee {
    Assert.notNull("id de l'activite", id);
    Assert.notNull("activite", activite);
    Assert.notNull("plage", plage);
    Assert.notNull("echeance", echeance);
  }

  public static IdentiteBuilder builder() {
    return id -> activite -> plage -> echeance -> new ActiviteInterpretee(id, activite, plage, echeance);
  }

  public interface IdentiteBuilder {
    ActiviteBuilder id(ActiviteId id);
  }

  public interface ActiviteBuilder {
    PlageBuilder activite(Activite activite);
  }

  public interface PlageBuilder {
    EcheanceBuilder plage(Plage plage);
  }

  public interface EcheanceBuilder {
    ActiviteInterpretee echeance(Instant echeance);
  }

  public Optional<TrancheDActivite> termineeA(Instant evaluation) {
    return plage
      .fin()
      .or(() -> evaluation.isBefore(echeance) ? Optional.empty() : Optional.of(echeance))
      .map(fin -> new TrancheDActivite(activite, new Periode(plage.debut(), fin), plage.fin().isEmpty()));
  }
}
