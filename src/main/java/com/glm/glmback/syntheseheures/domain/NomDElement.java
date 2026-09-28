package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Le nom de l'element, tel que l'atelier l'a copie a l'engagement.
 */
public record NomDElement(String value) {
  private static final int MAX_LENGTH = 30;

  public NomDElement {
    Assert.field("nom de l'element", value).notBlank().maxLength(MAX_LENGTH);
  }
}
