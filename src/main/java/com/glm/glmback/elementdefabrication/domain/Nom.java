package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.regex.Pattern;

public record Nom(String value) {
  private static final Pattern PATTERN = Pattern.compile("^[A-Z]{1,10}-\\d{4}-\\d{6,}$");
  private static final String FORMAT = "%s-%04d-%06d";

  public Nom {
    Assert.field("nom", value).notBlank().matches(PATTERN);
  }

  public static Nom of(Prefixe prefixe, Annee annee, long compteur) {
    return new Nom(FORMAT.formatted(prefixe.value(), annee.value(), compteur));
  }
}
