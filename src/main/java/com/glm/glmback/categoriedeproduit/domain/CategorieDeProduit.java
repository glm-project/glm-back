package com.glm.glmback.categoriedeproduit.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Ce que l'entreprise fabrique, range par familles qu'elle nomme elle-meme : des moules et des OF chez le client de
 * reference, autre chose ailleurs.
 */
public record CategorieDeProduit(CodeDeCategorie code, Rang rang) {
  public CategorieDeProduit {
    Assert.notNull("code", code);
    Assert.notNull("rang", rang);
  }

  public CategorieDeProduit deplace(Rang rang) {
    return new CategorieDeProduit(code, rang);
  }
}
