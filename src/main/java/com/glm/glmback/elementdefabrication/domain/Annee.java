package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.time.ZoneOffset;

public record Annee(int value) {
  private static final int PREMIERE_ANNEE = 2000;
  private static final int DERNIERE_ANNEE = 9999;

  public Annee {
    Assert.field("annee", value).min(PREMIERE_ANNEE).max(DERNIERE_ANNEE);
  }

  public static Annee de(Instant date) {
    return new Annee(date.atZone(ZoneOffset.UTC).getYear());
  }
}
