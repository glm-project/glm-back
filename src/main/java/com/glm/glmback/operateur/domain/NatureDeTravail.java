package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Le metier exerce sur un poste : soudage, tournage, fraisage, dessin.
 *
 * <p>
 * Ce contexte ne la declare jamais sur une personne : il la lit sur les postes auxquels l'operateur est habilite, et
 * en deduit ses metiers. Le type est redeclare ici plutot qu'importe du contexte des postes, annote
 * {@code @BusinessContext}.
 * </p>
 */
public record NatureDeTravail(String value) implements Comparable<NatureDeTravail> {
  private static final int MAX_LENGTH = 50;

  public NatureDeTravail {
    Assert.field("nature de travail", value).notBlank().maxLength(MAX_LENGTH);
  }

  @Override
  public int compareTo(NatureDeTravail other) {
    return value().compareTo(other.value());
  }
}
