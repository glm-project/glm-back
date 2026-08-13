package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record PosteDeTravailACreer(Libelle libelle, NatureDeTravail nature) {
  public PosteDeTravailACreer {
    Assert.notNull("libelle", libelle);
    Assert.notNull("nature de travail", nature);
  }

  public PosteDeTravailACreer(String libelle, String nature) {
    this(new Libelle(libelle), new NatureDeTravail(nature));
  }
}
