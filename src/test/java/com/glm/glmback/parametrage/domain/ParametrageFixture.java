package com.glm.glmback.parametrage.domain;

import java.time.Duration;

public final class ParametrageFixture {

  public static final DureeMaxDActivite DUREE_MAX_D_ACTIVITE_DIX_HEURES = new DureeMaxDActivite(Duration.ofHours(10));

  private ParametrageFixture() {}

  public static Parametrage parametrageDixHeures() {
    return new Parametrage(DUREE_MAX_D_ACTIVITE_DIX_HEURES);
  }
}
