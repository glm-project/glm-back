package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;

public record PosteDeTravailACreer(Libelle libelle, NatureDeTravailId nature, CoutHoraire coutHoraire) {
  public PosteDeTravailACreer {
    Assert.notNull("libelle", libelle);
    Assert.notNull("id de la nature de travail", nature);
    Assert.notNull("cout horaire", coutHoraire);
  }

  public PosteDeTravailACreer(String libelle, NatureDeTravailId nature, BigDecimal coutHoraire) {
    this(new Libelle(libelle), nature, new CoutHoraire(coutHoraire));
  }
}
