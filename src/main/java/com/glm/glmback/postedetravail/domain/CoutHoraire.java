package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Le cout horaire d'un poste de travail, destine au lot cout de revient.
 *
 * <p>
 * Exige a la declaration et a la revision : il est fige sur chaque pointage, et un pointage sans tarif rend le cout de
 * revient non exportable. A zero, le poste ne demande que de la main d'oeuvre. Un poste declare avant cette exigence
 * peut encore en etre depourvu, d'ou {@link #of(BigDecimal)} a la relecture.
 * </p>
 */
public record CoutHoraire(BigDecimal value) {
  public CoutHoraire {
    Assert.field("cout horaire", value).notNull().positive().under(100_000_000);
    Assert.field("decimales du cout horaire", value.stripTrailingZeros().scale()).max(2);
  }

  public static Optional<CoutHoraire> of(BigDecimal value) {
    return Optional.ofNullable(value).map(CoutHoraire::new);
  }
}
