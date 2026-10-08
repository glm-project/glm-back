package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.List;

/**
 * Un suivi tel qu'il se lit a un instant d'evaluation.
 *
 * <p>
 * Son etat et ses activites en cours y jugent l'echeance de chaque activite : une activite que rien n'a terminee en
 * sort des que son echeance est atteinte. L'instant vient de l'horloge de celui qui lit ; le suivi, lui, ne depend que
 * de son journal.
 * </p>
 */
public record LectureDuSuivi(SuiviDAtelier suivi, Instant evaluation) {
  public LectureDuSuivi {
    Assert.notNull("suivi", suivi);
    Assert.notNull("evaluation", evaluation);
  }

  public EtatDAtelier etat() {
    return suivi.etat(evaluation);
  }

  public List<ActiviteEnCours> activitesEnCours() {
    return suivi.activitesEnCours(evaluation);
  }
}
