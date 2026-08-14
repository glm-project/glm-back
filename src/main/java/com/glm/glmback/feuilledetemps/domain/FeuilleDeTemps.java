package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;

/**
 * L'historique calendaire d'un operateur sur une semaine.
 *
 * <p>
 * Rien n'est stocke : la feuille est recalculee a chaque lecture depuis les journaux de l'atelier. Une saisie
 * regularisee hier pour la semaine derniere doit apparaitre a l'heure ou le travail a eu lieu, ce qu'aucune
 * projection figee ne saurait rattraper.
 * </p>
 */
public record FeuilleDeTemps(OperateurConnu operateur, SemaineCalendaire semaine, List<JourDeLaSemaine> jours) {
  public FeuilleDeTemps {
    Assert.notNull("operateur", operateur);
    Assert.notNull("semaine", semaine);
    Assert.field("jours", jours).notNull().noNullElement();
  }
}
