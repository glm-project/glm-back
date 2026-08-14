package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;

public record PosteDeTravailAModifier(PosteDeTravailId id, Libelle libelle, NatureDeTravail nature, Optional<CoutHoraire> coutHoraire) {
  public PosteDeTravailAModifier {
    Assert.notNull("id", id);
    Assert.notNull("libelle", libelle);
    Assert.notNull("nature de travail", nature);
    Assert.notNull("cout horaire", coutHoraire);
  }

  public PosteDeTravailAModifier(PosteDeTravailId id, String libelle, String nature, BigDecimal coutHoraire) {
    this(id, new Libelle(libelle), new NatureDeTravail(nature), CoutHoraire.of(coutHoraire));
  }
}
