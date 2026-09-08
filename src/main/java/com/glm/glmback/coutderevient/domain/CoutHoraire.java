package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Le cout horaire du poste, tel que le journal d'atelier l'a fige au moment de la saisie.
 *
 * <p>
 * Il n'est jamais divise : le client enonce la regle deux fois, le cout de chaque machine active court en entier,
 * meme quand l'operateur en mene plusieurs de front. Facultatif comme sa source, absent plutot qu'a zero.
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
