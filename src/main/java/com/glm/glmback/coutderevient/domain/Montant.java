package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Une somme d'argent, arrondie au centime.
 *
 * <p>
 * C'est le seul endroit ou le calcul quitte son echelle de travail. Le rapport arrondit par ligne, puis totalise des
 * lignes deja arrondies : sans quoi l'ecran afficherait un total qui ne serait pas la somme de ce qu'il montre.
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
