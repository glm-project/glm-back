package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.UUID;

/**
 * Le poste pointe par l'operateur. C'est lui, et non la nature de l'operation, qui distingue deux activites menees de
 * front sur le meme element.
 */
public record PosteDeTravailId(UUID uuid) {
  public PosteDeTravailId {
    Assert.notNull("id du poste de travail", uuid);
  }
}
