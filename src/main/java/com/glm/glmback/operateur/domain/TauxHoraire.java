package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Le taux horaire de l'operateur, destine au lot cout de revient.
 *
 * <p>
 * Facultatif, comme le matricule : une entreprise qui ne valorise pas encore ses operateurs retrouve un comportement
 * coherent plutot qu'un cas degrade.
 * </p>
 */
public record TauxHoraire(BigDecimal value) {
  public TauxHoraire {
    Assert.field("taux horaire", value).notNull().strictlyPositive();
  }

  public static Optional<TauxHoraire> of(BigDecimal value) {
    return Optional.ofNullable(value).map(TauxHoraire::new);
  }
}
