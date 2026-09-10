package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Le metier qui s'exerce sur le poste — fraisage, tournage, erosion —, copie du poste au moment de la saisie.
 *
 * <p>
 * C'est l'axe d'agregation du rapport : une ligne par nature. Elle reste facultative, comme le poste dont elle vient.
 * </p>
 */
public record NatureDOperation(String value) {
  private static final int MAX_LENGTH = 50;

  public NatureDOperation {
    Assert.field("nature de l'operation", value).notBlank().maxLength(MAX_LENGTH);
  }
}
