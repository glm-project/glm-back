package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

/**
 * Couple bitemporel d'une saisie : quand le fait a eu lieu, et quand il a ete enregistre.
 *
 * <p>
 * Un pointage en temps reel porte deux dates egales ; une regularisation enregistre aujourd'hui un fait survenu hier.
 * C'est ce qui permet a une saisie oubliee de compter a l'heure ou elle a eu lieu.
 * </p>
 */
public record Horodatage(Instant dateDeSurvenue, Instant dateDEnregistrement) {
  public Horodatage {
    Assert.notNull("dateDeSurvenue", dateDeSurvenue);
    Assert.field("dateDEnregistrement", dateDEnregistrement).afterOrAt(dateDeSurvenue);
  }

  public static Horodatage saisiA(Instant date) {
    return new Horodatage(date, date);
  }

  /**
   * Vrai si le fait a ete enregistre apres coup. C'est le seul critere qui distingue une regularisation d'un pointage
   * en direct : l'identite de l'auteur, elle, dit qui a saisi, pas quand.
   */
  public boolean estDifferee() {
    return !dateDEnregistrement.equals(dateDeSurvenue);
  }
}
