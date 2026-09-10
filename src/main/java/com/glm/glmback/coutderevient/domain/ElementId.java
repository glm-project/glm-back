package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * L'identite de l'element de fabrication dont le cout de revient est lu.
 *
 * <p>
 * Ce contexte entre par l'element, non par son suivi d'atelier : un element reengage apres cloture a deux suivis, et
 * son cout de revient est celui de tout ce qui a ete fait dessus.
 * </p>
 */
public record ElementId(UUID uuid) {
  public ElementId {
    Assert.notNull("id de l'element de fabrication", uuid);
  }
}
