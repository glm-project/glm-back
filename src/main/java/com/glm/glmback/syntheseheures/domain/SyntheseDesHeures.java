package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.util.List;

/**
 * Le releve des heures d'un operateur sur une semaine.
 *
 * <p>
 * Rien n'est stocke : la synthese est recalculee a chaque lecture depuis les journaux de l'atelier, pour qu'une
 * saisie regularisee apres coup compte a l'heure ou le travail a eu lieu.
 * </p>
 */
public record SyntheseDesHeures(OperateurConnu operateur, SemaineCalendaire semaine, List<JourDeSynthese> jours) {
  public SyntheseDesHeures {
    Assert.notNull("operateur", operateur);
    Assert.notNull("semaine", semaine);
    Assert.field("jours", jours).notNull().noNullElement();
  }

  /**
   * La duree travaillee de la semaine, somme des sept jours.
   */
  public Duration dureeTotale() {
    return jours.stream().map(JourDeSynthese::duree).reduce(Duration.ZERO, Duration::plus);
  }

  /**
   * Vrai des qu'un jour de la semaine porte une anomalie — le signal transmis au lecteur du releve qu'une
   * correction est attendue quelque part dans la semaine.
   */
  public boolean aUneAnomalie() {
    return jours.stream().anyMatch(JourDeSynthese::aUneAnomalie);
  }
}
