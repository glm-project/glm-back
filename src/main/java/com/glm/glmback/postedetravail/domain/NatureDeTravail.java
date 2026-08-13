package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Le metier qui s'exerce sur un poste : soudage, tournage, fraisage, dessin.
 *
 * <p>
 * Elle appartient au poste, jamais a la personne : c'est le poste choisi au pointage qui dit quel metier est exerce a
 * cet instant. Les metiers d'un operateur se deduisent des postes sur lesquels il est habilite.
 * </p>
 */
public record NatureDeTravail(String value) {
  private static final int MAX_LENGTH = 50;

  public NatureDeTravail {
    Assert.field("nature de travail", value).notBlank().maxLength(MAX_LENGTH);
  }
}
