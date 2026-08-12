package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record EngagementAEnregistrer(ElementEngageId element, Auteur auteur) {
  public EngagementAEnregistrer {
    Assert.notNull("element", element);
    Assert.notNull("auteur", auteur);
  }
}
