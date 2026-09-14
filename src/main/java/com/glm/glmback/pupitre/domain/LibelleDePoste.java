package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record LibelleDePoste(String value) {
  private static final int MAX_LENGTH = 100;

  public LibelleDePoste {
    Assert.field("libelle du poste", value).notBlank().maxLength(MAX_LENGTH);
  }
}
