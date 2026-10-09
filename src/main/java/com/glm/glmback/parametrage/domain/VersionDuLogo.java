package com.glm.glmback.parametrage.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.regex.Pattern;

/**
 * L'empreinte du contenu d'un logo : deux logos identiques ont la meme, un nouveau logo en a une autre. Elle entre dans
 * l'adresse de l'image, pour que le navigateur la garde en cache tant qu'elle ne change pas.
 */
public record VersionDuLogo(String value) {
  private static final Pattern MOTIF = Pattern.compile("^[0-9a-f]{16}$");

  public VersionDuLogo {
    Assert.field("version", value).notBlank().matches(MOTIF);
  }
}
