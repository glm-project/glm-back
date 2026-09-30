package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/** Les bornes factuelles projetees par atelier, sans relecture concurrente du journal. */
public record ActiviteInterpretee(ActiviteId id, Activite activite, Plage plage, Instant echeance, Optional<Instant> finAuPlusTard) {
  public ActiviteInterpretee {
    Assert.notNull("id de l'activite", id);
    Assert.notNull("activite", activite);
    Assert.notNull("plage", plage);
    Assert.notNull("echeance", echeance);
    Assert.notNull("fin au plus tard", finAuPlusTard);
  }

  public static IdentiteBuilder builder() {
    return id -> activite -> plage -> echeance -> finAuPlusTard -> new ActiviteInterpretee(id, activite, plage, echeance, finAuPlusTard);
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
    FinBuilder echeance(Instant echeance);
  }

  public interface FinBuilder {
    ActiviteInterpretee finAuPlusTard(Optional<Instant> finAuPlusTard);
  }

  public boolean aResoudre() {
    return finAuPlusTard.isPresent();
  }

  public Optional<ZoneIncertaine> zoneA(Instant evaluation) {
    return finAuPlusTard
      .map(fin -> fin.isBefore(evaluation) ? fin : evaluation)
      .filter(fin -> fin.isAfter(plage.debut()))
      .map(fin -> new ZoneIncertaine(this, new Periode(plage.debut(), fin)));
  }

  public Optional<TrancheDActivite> termineeA(Instant evaluation) {
    if (aResoudre()) {
      return Optional.empty();
    }
    return plage
      .fin()
      .or(() -> evaluation.isBefore(echeance) ? Optional.empty() : Optional.of(echeance))
      .map(fin -> new TrancheDActivite(activite, new Periode(plage.debut(), fin), plage.fin().isEmpty()));
  }
}
