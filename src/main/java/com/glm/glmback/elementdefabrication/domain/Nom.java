package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Nom(String value) {
  private static final Pattern PATTERN = Pattern.compile("^([A-Z]{1,10})-(\\d{4})-(\\d{6,})$");
  private static final String FORMAT = "%s-%04d-%06d";
  private static final int GROUPE_PREFIXE = 1;
  private static final int GROUPE_ANNEE = 2;
  private static final int GROUPE_COMPTEUR = 3;

  public Nom {
    Assert.field("nom", value).notBlank().matches(PATTERN);
  }

  public static Nom of(Prefixe prefixe, Annee annee, long compteur) {
    return new Nom(FORMAT.formatted(prefixe.value(), annee.value(), compteur));
  }

  public Prefixe prefixe() {
    return new Prefixe(groupe(GROUPE_PREFIXE));
  }

  public Annee annee() {
    return new Annee(Integer.parseInt(groupe(GROUPE_ANNEE)));
  }

  public long compteur() {
    return Long.parseLong(groupe(GROUPE_COMPTEUR));
  }

  private String groupe(int index) {
    Matcher matcher = PATTERN.matcher(value);
    matcher.matches();

    return matcher.group(index);
  }
}
