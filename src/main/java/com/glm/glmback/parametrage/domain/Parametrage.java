package com.glm.glmback.parametrage.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Les reglages que l'entreprise fixe elle-meme, une seule fois pour toute l'entreprise.
 */
public record Parametrage(DureeMaxDActivite dureeMaxDActivite) {
  public Parametrage {
    Assert.notNull("dureeMaxDActivite", dureeMaxDActivite);
  }

  public Parametrage fixeLaDureeMaxDActivite(DureeMaxDActivite duree) {
    return new Parametrage(duree);
  }
}
