package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Regles de selection de la liste des anomalies : l'instant et le seuil qui les definissent, et deux filtres
 * facultatifs, l'operateur et le type.
 */
public record CriteresDAnomalie(
  Instant maintenant,
  AmplitudeMaximale seuil,
  Optional<OperateurId> operateur,
  Optional<TypeDAnomalie> type
) {
  public CriteresDAnomalie {
    Assert.notNull("maintenant", maintenant);
    Assert.notNull("seuil", seuil);
    Assert.notNull("operateur", operateur);
    Assert.notNull("type", type);
  }

  public boolean matches(JourneeDeTravail journee) {
    return (
      operateur.map(journee.operateur()::equals).orElse(true)
      && AnomalieDePresence.de(journee, maintenant, seuil)
        .filter(anomalie -> type.map(anomalie.type()::equals).orElse(true))
        .isPresent()
    );
  }
}
