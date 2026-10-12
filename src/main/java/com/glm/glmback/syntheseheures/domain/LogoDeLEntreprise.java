package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Le logo de l'entreprise, tel que le parametrage l'a valide au depot. Le rapport le relit a chaque generation : un
 * logo remplace s'affiche des le document suivant.
 */
public final class LogoDeLEntreprise {

  private final byte[] contenu;
  private final FormatDuLogo format;

  public LogoDeLEntreprise(byte[] contenu, FormatDuLogo format) {
    Assert.notNull("contenu", contenu);
    Assert.notNull("format", format);
    this.contenu = contenu.clone();
    this.format = format;
  }

  public byte[] contenu() {
    return contenu.clone();
  }

  public FormatDuLogo format() {
    return format;
  }
}
