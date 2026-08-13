package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record LibelleDePoste(String value) implements Comparable<LibelleDePoste> {
  private static final int MAX_LENGTH = 100;

  public LibelleDePoste {
    Assert.field("libelle du poste", value).notBlank().maxLength(MAX_LENGTH);
  }

  @Override
  public int compareTo(LibelleDePoste other) {
    return value().compareTo(other.value());
  }
}
