package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.activityduration.domain.MaximumActivityDuration;
import com.glm.glmback.shared.activityduration.domain.MaximumActivityDurations;
import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

/**
 * L'instant auquel une activite que rien n'a terminee se termine automatiquement : son debut plus la duree maximale
 * d'activite en vigueur quand elle a commence.
 *
 * <p>
 * Des heures ecoulees, jamais des heures d'horloge murale : l'activite n'a pas de fuseau, et le passage a l'heure d'ete
 * ou d'hiver ne l'allonge ni ne la raccourcit. La duree est un reglage de l'entreprise, que le gestionnaire fixe et que
 * le pupitre lit au referentiel : l'atelier la recoit par le port {@link MaximumActivityDurations} et la copie sur
 * l'evenement qui ouvre l'activite, qui la porte pour toujours. Ce domaine ne connait aucune valeur par defaut. Une
 * fin reelle peut toujours remplacer l'echeance.
 * </p>
 */
public record Echeance(Instant value) {
  public Echeance {
    Assert.notNull("echeance", value);
  }

  public static Echeance apres(Instant debut, MaximumActivityDuration duree) {
    return new Echeance(debut.plus(duree.value()));
  }

  /**
   * Vrai a partir de l'echeance elle-meme : a 21:00:00 pile, une activite commencee a 8 h n'est plus en cours.
   */
  public boolean estAtteinteA(Instant evaluation) {
    return !evaluation.isBefore(value);
  }
}
