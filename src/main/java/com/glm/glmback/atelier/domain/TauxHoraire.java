package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Le taux horaire de l'operateur, copie du referentiel au moment de la saisie.
 *
 * <p>
 * Sur le meme patron que {@link NatureDOperation} : jamais relu depuis {@code operateur} apres coup, pour qu'une
 * revalorisation ne reecrive pas l'histoire d'un pointage deja fait. Facultatif, comme sa source : strictement
 * positif quand il est renseigne, absent plutot qu'a zero quand l'operateur n'est pas valorise.
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
