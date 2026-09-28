package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * L'element de fabrication sur lequel l'operateur a travaille, jamais le suivi qui l'a porte en atelier : un element
 * reengage apres cloture reste le meme element.
 */
public record ElementId(UUID uuid) {
  public ElementId {
    Assert.notNull("id de l'element de fabrication", uuid);
  }
}
