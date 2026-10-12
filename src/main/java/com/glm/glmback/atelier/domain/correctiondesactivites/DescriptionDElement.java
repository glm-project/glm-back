package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * La description d'un element de fabrication, relue au referentiel a chaque lecture.
 */
public record DescriptionDElement(String value) {
  public DescriptionDElement {
    Assert.field("description de l'element", value).notBlank();
  }
}
