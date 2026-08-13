package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record PosteDeTravailAModifier(PosteDeTravailId id, Libelle libelle, NatureDeTravail nature) {
  public PosteDeTravailAModifier {
    Assert.notNull("id", id);
    Assert.notNull("libelle", libelle);
    Assert.notNull("nature de travail", nature);
  }

  public PosteDeTravailAModifier(PosteDeTravailId id, String libelle, String nature) {
    this(id, new Libelle(libelle), new NatureDeTravail(nature));
  }
}
