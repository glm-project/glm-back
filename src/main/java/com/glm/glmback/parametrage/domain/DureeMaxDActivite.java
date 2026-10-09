package com.glm.glmback.parametrage.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;

/**
 * Le temps au bout duquel une activite que rien n'a terminee se termine automatiquement : treize heures tant que
 * l'entreprise n'en a pas decide autrement, jamais moins d'une heure ni plus d'une journee.
 */
public record DureeMaxDActivite(Duration value) {
  private static final Duration MINIMUM = Duration.ofHours(1);
  private static final Duration MAXIMUM = Duration.ofHours(24);
  private static final Duration PAR_DEFAUT = Duration.ofHours(13);

  public DureeMaxDActivite {
    Assert.notNull("dureeMaxDActivite", value);
    Assert.field("dureeMaxDActivite", value.toSeconds()).min(MINIMUM.toSeconds()).max(MAXIMUM.toSeconds());
  }

  public static DureeMaxDActivite parDefaut() {
    return new DureeMaxDActivite(PAR_DEFAUT);
  }
}
