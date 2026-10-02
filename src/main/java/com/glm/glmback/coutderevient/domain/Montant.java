package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Une somme d'argent, arrondie au centime.
 *
 * <p>
 * Le calcul quitte ici son echelle de travail : la machine une fois par activite, la main d'oeuvre une fois par
 * fenetre de partage avant d'etre repartie au centime. Lignes et rapport ne totalisent que des montants deja
 * arrondis : sans quoi l'ecran afficherait un total qui ne serait pas la somme de ce qu'il montre.
 * </p>
 */
public record Montant(BigDecimal value) {
  private static final int DECIMALES = 2;

  public static final Montant ZERO = new Montant(BigDecimal.ZERO);

  public Montant {
    Assert.field("montant", value).notNull().positive();
    value = value.setScale(DECIMALES, RoundingMode.HALF_UP);
  }

  public Montant plus(Montant autre) {
    return new Montant(value.add(autre.value));
  }
}
