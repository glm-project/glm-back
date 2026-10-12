package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * La reference d'un element de fabrication, relue au referentiel a chaque lecture.
 */
public record ReferenceDElement(String value) {
  public ReferenceDElement {
    Assert.field("reference de l'element", value).notBlank();
  }
}
