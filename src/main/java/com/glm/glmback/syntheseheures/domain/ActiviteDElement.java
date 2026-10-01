package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;

/** L'element engage et son activite interpretee par atelier. */
public record ActiviteDElement(ElementEngage element, ActiviteInterpretee activite) {
  public ActiviteDElement {
    Assert.notNull("element", element);
    Assert.notNull("activite", activite);
  }
}
