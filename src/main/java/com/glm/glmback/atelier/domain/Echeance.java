package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.time.Instant;

/**
 * L'instant auquel une activite que rien n'a terminee se termine automatiquement : son debut plus treize heures.
 *
 * <p>
 * Treize heures ecoulees, jamais treize heures d'horloge murale : l'activite n'a pas de fuseau, et le passage a l'heure
 * d'ete ou d'hiver ne l'allonge ni ne la raccourcit. Le delai n'est pas un parametre de l'entreprise : c'est la regle
 * de l'atelier, qui fixe une borne par defaut a toute activite oubliee, qu'une fin reelle peut toujours remplacer.
 * </p>
 */
public record Echeance(Instant value) {
  private static final Duration DELAI = Duration.ofHours(13);

  public Echeance {
    Assert.notNull("echeance", value);
  }

  public static Echeance apres(Instant debut) {
    return new Echeance(debut.plus(DELAI));
  }

  /**
   * Vrai a partir de l'echeance elle-meme : a 21:00:00 pile, une activite commencee a 8 h n'est plus en cours.
   */
  public boolean estAtteinteA(Instant evaluation) {
    return !evaluation.isBefore(value);
  }
}
