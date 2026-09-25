package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Une journee de travail que le gestionnaire doit regarder.
 *
 * <p>
 * Rien n'est stocke : l'anomalie se deduit de la journee, du seuil et de l'instant de lecture. Une regularisation qui
 * la resout la fait donc disparaitre d'elle-meme, et un seuil change la fait apparaitre ou disparaitre sans migration.
 * </p>
 */
public record AnomalieDePresence(TypeDAnomalie type, JourneeDeTravail journee) {
  public AnomalieDePresence {
    Assert.notNull("type", type);
    Assert.notNull("journee", journee);
  }

  /**
   * L'anomalie de cette journee a cet instant, s'il y en a une : abandonnee sans depart, ou fermee au-dela du seuil.
   * Le seuil pile n'est pas depasse.
   */
  public static Optional<AnomalieDePresence> de(JourneeDeTravail journee, Instant maintenant, AmplitudeMaximale seuil) {
    if (journee.estAbandonneePour(maintenant, seuil)) {
      return Optional.of(new AnomalieDePresence(TypeDAnomalie.JOURNEE_SANS_DEPART, journee));
    }

    return journee
      .amplitude()
      .map(periode -> Duration.between(periode.debut(), periode.fin()))
      .filter(amplitude -> amplitude.compareTo(seuil.value()) > 0)
      .map(amplitude -> new AnomalieDePresence(TypeDAnomalie.AMPLITUDE_EXCESSIVE, journee));
  }

  public Instant arrivee() {
    return journee.debut().orElseThrow();
  }

  public Optional<Instant> depart() {
    return journee.amplitude().map(Periode::fin);
  }

  public Optional<Duration> amplitude() {
    return journee.amplitude().map(periode -> Duration.between(periode.debut(), periode.fin()));
  }
}
