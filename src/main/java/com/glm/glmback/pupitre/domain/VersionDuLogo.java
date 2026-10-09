package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.regex.Pattern;

/**
 * La version du logo de l'entreprise : le pupitre ne telecharge l'image que quand elle change, et la garde hors ligne.
 */
public record VersionDuLogo(String value) {
  private static final Pattern MOTIF = Pattern.compile("^[0-9a-f]{16}$");

  public VersionDuLogo {
    Assert.field("version du logo", value).notBlank().matches(MOTIF);
  }
}
