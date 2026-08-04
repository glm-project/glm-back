package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record Nom(String value) {
  private static final String FORMAT = "%s-%04d-%06d";

  public Nom {
    Assert.field("nom", value).notBlank();
  }

  public static Nom of(Prefixe prefixe, Annee annee, long numero) {
    return new Nom(FORMAT.formatted(prefixe.value(), annee.value(), numero));
  }
}
