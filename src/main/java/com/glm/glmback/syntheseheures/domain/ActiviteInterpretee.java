package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/** Les faits projetes par atelier ; seule l'expiration depend de l'instant de lecture. */
public record ActiviteInterpretee(ActiviteId id, Activite activite, Plage plage, Instant echeance, boolean aResoudre) {
  public ActiviteInterpretee {
    Assert.notNull("id de l'activite", id);
    Assert.notNull("activite", activite);
    Assert.notNull("plage", plage);
    Assert.notNull("echeance", echeance);
  }

  public static IdentiteBuilder builder() {
    return id -> activite -> plage -> echeance -> aResoudre -> new ActiviteInterpretee(id, activite, plage, echeance, aResoudre);
  }

  public IntervalleDActivite a(Instant evaluation) {
    EtatDActivite etat;
    Plage lue = plage;
    if (aResoudre) {
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
    return new IntervalleDActivite(activite, lue, new ActiviteLue(id, etat, lue));
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
    AResoudreBuilder echeance(Instant echeance);
  }

  public interface AResoudreBuilder {
    ActiviteInterpretee aResoudre(boolean aResoudre);
  }
}
