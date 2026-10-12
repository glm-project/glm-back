package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Le taux horaire de l'operateur, destine au lot cout de revient.
 *
 * <p>
 * Exige a la declaration et a la revision : il est fige sur chaque pointage, et un pointage sans tarif rend le cout de
 * revient non exportable. Un operateur declare avant cette exigence peut encore en etre depourvu, d'ou
 * {@link #of(BigDecimal)} a la relecture.
 * </p>
 */
public record TauxHoraire(BigDecimal value) {
  public TauxHoraire {
    Assert.field("taux horaire", value).notNull().strictlyPositive().under(100_000_000);
    Assert.field("decimales du taux horaire", value.stripTrailingZeros().scale()).max(2);
  }

  public static Optional<TauxHoraire> of(BigDecimal value) {
    return Optional.ofNullable(value).map(TauxHoraire::new);
  }
}
