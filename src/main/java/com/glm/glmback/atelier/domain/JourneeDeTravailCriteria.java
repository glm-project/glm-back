package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Regles de selection de l'historique de presence.
 *
 * <p>
 * Les criteres sont facultatifs : la supervision selectionne les venues ouvertes par leur etat, sans periode
 * calendaire qui masquerait une arrivee ancienne ou une venue traversant minuit.
 * </p>
 */
public record JourneeDeTravailCriteria(Optional<Periode> periode, Optional<OperateurId> operateur, Optional<EtatDePresence> etat) {
  public JourneeDeTravailCriteria {
    Assert.notNull("periode", periode);
    Assert.notNull("operateur", operateur);
    Assert.notNull("etat", etat);
  }

  public JourneeDeTravailCriteria(Optional<Periode> periode, Optional<OperateurId> operateur) {
    this(periode, operateur, Optional.empty());
  }

  public boolean matches(JourneeDeTravail journee) {
    return correspondALOperateur(journee) && correspondALaPeriode(journee) && etat.map(journee.etat()::equals).orElse(true);
  }

  private boolean correspondALOperateur(JourneeDeTravail journee) {
    return operateur.map(journee.operateur()::equals).orElse(true);
  }

  private boolean correspondALaPeriode(JourneeDeTravail journee) {
    return periode.map(bornes -> journee.debut().filter(bornes::contains).isPresent()).orElse(true);
  }
}
