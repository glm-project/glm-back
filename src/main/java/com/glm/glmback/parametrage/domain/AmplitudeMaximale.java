package com.glm.glmback.parametrage.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;

/**
 * La duree au-dela de laquelle une journee de travail sans depart est abandonnee.
 *
 * <p>
 * Elle se compte a la minute et reste strictement sous 24 h : c'est ce qui garantit qu'un retour le lendemain a la
 * meme heure soit toujours une nouvelle arrivee, meme pour un poste de nuit.
 * </p>
 */
public record AmplitudeMaximale(Duration value) {
  private static final long MINUTES_PAR_JOUR = Duration.ofDays(1).toMinutes();
  private static final long NANOS_PAR_MINUTE = Duration.ofMinutes(1).toNanos();

  public AmplitudeMaximale {
    Assert.notNull("amplitude maximale", value);
    Assert.field("amplitude maximale", value.toMinutes()).min(1).max(MINUTES_PAR_JOUR - 1);
    Assert.field("secondes de l'amplitude maximale", value.toNanos() % NANOS_PAR_MINUTE).max(0);
  }
}
