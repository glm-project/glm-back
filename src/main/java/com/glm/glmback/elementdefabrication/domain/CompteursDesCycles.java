package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Le nombre d'elements de chaque cycle pour un meme filtre : les clotures ne se comptent que sur leur periode de
 * cloture, quand elle est donnee.
 */
public record CompteursDesCycles(long enPreparation, long enAtelier, long clotures) {
  public CompteursDesCycles {
    Assert.field("enPreparation", enPreparation).positive();
    Assert.field("enAtelier", enAtelier).positive();
    Assert.field("clotures", clotures).positive();
  }
}
