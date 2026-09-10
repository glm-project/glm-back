package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;

/**
 * Le nombre de postes de travail qu'un operateur occupe simultanement, tous elements confondus.
 *
 * <p>
 * Un pointage sans poste compte pour un poste : c'est la cle d'activite de l'atelier, ou l'absence de poste est une
 * valeur comme une autre. Il vaut donc toujours au moins un — la ou personne ne travaille, aucune periode ne se
 * construit.
 * </p>
 */
public record Diviseur(int value) {
  private static final int MINIMUM = 1;

  public Diviseur {
    Assert.field("diviseur", value).min(MINIMUM);
  }
}
