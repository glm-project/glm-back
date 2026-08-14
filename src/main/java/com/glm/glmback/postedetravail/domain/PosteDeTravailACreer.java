package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;

public record PosteDeTravailACreer(Libelle libelle, NatureDeTravail nature, Optional<CoutHoraire> coutHoraire) {
  public PosteDeTravailACreer {
    Assert.notNull("libelle", libelle);
    Assert.notNull("nature de travail", nature);
    Assert.notNull("cout horaire", coutHoraire);
  }

  public PosteDeTravailACreer(String libelle, String nature, BigDecimal coutHoraire) {
    this(new Libelle(libelle), new NatureDeTravail(nature), CoutHoraire.of(coutHoraire));
  }
}
