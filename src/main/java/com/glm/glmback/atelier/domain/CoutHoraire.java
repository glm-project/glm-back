package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Le cout horaire du poste, copie du referentiel au moment de la saisie.
 *
 * <p>
 * Sur le meme patron que {@link NatureDOperation} : jamais relu depuis {@code postedetravail} apres coup, pour qu'un
 * poste requalifie plus tard ne reecrive pas l'histoire d'un pointage deja fait. Facultatif, comme sa source :
 * strictement positif quand il est renseigne, absent plutot qu'a zero quand le poste n'est pas valorise.
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
