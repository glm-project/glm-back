package com.glm.glmback.parametrage.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class ParametrageFixture {

  public static final AmplitudeMaximale AMPLITUDE_MAXIMALE_13H = new AmplitudeMaximale(Duration.ofHours(13));
  public static final AmplitudeMaximale AMPLITUDE_MAXIMALE_10H = new AmplitudeMaximale(Duration.ofHours(10));
  public static final AmplitudeMaximale AMPLITUDE_MAXIMALE_12H30 = new AmplitudeMaximale(Duration.ofMinutes(750));
  public static final Auteur AUTEUR_LEROY = new Auteur("leroy");
  public static final Auteur AUTEUR_MARTIN = new Auteur("martin");
  public static final Instant LE_24_SEPTEMBRE_2026_A_9H = Instant.parse("2026-09-24T09:00:00Z");
  public static final Instant LE_25_SEPTEMBRE_2026_A_9H = Instant.parse("2026-09-25T09:00:00Z");

  private ParametrageFixture() {}

  public static Parametrage parametrageParDefaut() {
    return new Parametrage(AMPLITUDE_MAXIMALE_13H, Optional.empty());
  }

  public static Modification modificationParLeroyLe24Septembre() {
    return new Modification(AUTEUR_LEROY, LE_24_SEPTEMBRE_2026_A_9H);
  }

  public static Modification modificationParMartinLe25Septembre() {
    return new Modification(AUTEUR_MARTIN, LE_25_SEPTEMBRE_2026_A_9H);
  }

  public static Parametrage parametrageA10HParLeroyLe24Septembre() {
    return new Parametrage(AMPLITUDE_MAXIMALE_10H, Optional.of(modificationParLeroyLe24Septembre()));
  }
}
