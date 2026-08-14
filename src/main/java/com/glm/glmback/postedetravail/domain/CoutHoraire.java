package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Le cout horaire d'un poste de travail, destine au lot cout de revient.
 *
 * <p>
 * Facultatif, comme le libelle et la nature ne le sont pas : une entreprise qui ne valorise pas encore ses postes
 * retrouve un comportement coherent plutot qu'un cas degrade.
 * </p>
 */
public record CoutHoraire(BigDecimal value) {
  public CoutHoraire {
    Assert.field("cout horaire", value).notNull().strictlyPositive();
  }

  public static Optional<CoutHoraire> of(BigDecimal value) {
    return Optional.ofNullable(value).map(CoutHoraire::new);
  }
}
