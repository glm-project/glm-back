package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/** Les faits projetes par atelier ; seule l'expiration depend de l'instant de lecture. */
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

  public IntervalleDActivite a(Instant evaluation) {
    EtatDActivite etat;
    Plage lue = plage;
    if (finAuPlusTard.isPresent()) {
      etat = EtatDActivite.A_RESOUDRE;
      lue = new Plage(plage.debut(), Optional.empty());
    } else if (plage.fin().isPresent()) {
      etat = EtatDActivite.TERMINEE;
    } else if (!evaluation.isBefore(echeance)) {
      etat = EtatDActivite.TERMINEE_AUTOMATIQUEMENT;
      lue = new Plage(plage.debut(), Optional.of(echeance));
    } else {
      etat = EtatDActivite.EN_COURS;
    }
    return new IntervalleDActivite(activite, lue, ActiviteLue.builder().id(id).etat(etat).plage(lue).finAuPlusTard(finAuPlusTard));
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
    FinAuPlusTardBuilder echeance(Instant echeance);
  }

  public interface FinAuPlusTardBuilder {
    ActiviteInterpretee finAuPlusTard(Optional<Instant> finAuPlusTard);
  }
}
