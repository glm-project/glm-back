package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Le taux horaire de l'operateur, tel que le journal d'atelier l'a fige au moment de la saisie.
 *
 * <p>
 * C'est le seul des deux qui se divise : une personne ne peut pas etre payee deux fois la meme heure, quel que soit
 * le nombre de machines qu'elle surveille. Facultatif comme sa source, absent plutot qu'a zero.
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
