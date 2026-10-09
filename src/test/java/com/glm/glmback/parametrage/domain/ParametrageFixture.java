package com.glm.glmback.parametrage.domain;

import java.time.Duration;

public final class ParametrageFixture {

  public static final DureeMaxDActivite DUREE_MAX_D_ACTIVITE_DIX_HEURES = new DureeMaxDActivite(Duration.ofHours(10));

  public static final VersionDuLogo VERSION_DU_LOGO_0123 = new VersionDuLogo("0123456789abcdef");

  private ParametrageFixture() {}

  public static Logo logoPngBleu() {
    return new Logo(ImagesFixture.pngCarre50(), FormatDImage.PNG, VERSION_DU_LOGO_0123);
  }

  public static Parametrage parametrageDixHeures() {
    return new Parametrage(DUREE_MAX_D_ACTIVITE_DIX_HEURES);
  }
}
