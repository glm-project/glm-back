package com.glm.glmback.categoriedeproduit.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Place d'une categorie dans l'ordre d'affichage choisi par l'entreprise, a partir de 1.
 */
public record Rang(int value) {
  private static final int PREMIER = 1;

  public Rang {
    Assert.field("rang", value).min(PREMIER);
  }

  public static Rang premier() {
    return new Rang(PREMIER);
  }

  public Rang suivant() {
    return new Rang(value + 1);
  }
}
