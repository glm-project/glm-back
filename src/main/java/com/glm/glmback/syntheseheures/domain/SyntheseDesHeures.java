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
public record SyntheseDesHeures(
  OperateurConnu operateur,
  SemaineCalendaire semaine,
  List<JourDeSynthese> jours,
  List<ElementDeLaSynthese> elements
) {
  public SyntheseDesHeures {
    Assert.notNull("operateur", operateur);
    Assert.notNull("semaine", semaine);
    Assert.field("jours", jours).notNull().noNullElement();
    Assert.field("elements", elements).notNull().noNullElement();
  }

  static SyntheseDesHeuresOperateurBuilder builder() {
    return operateur -> semaine -> jours -> elements -> new SyntheseDesHeures(operateur, semaine, jours, elements);
  }

  public Duration dureeOperationnelleTotale() {
    return jours.stream().map(JourDeSynthese::dureeOperationnelle).reduce(Duration.ZERO, Duration::plus);
  }

  interface SyntheseDesHeuresOperateurBuilder {
    SyntheseDesHeuresSemaineBuilder operateur(OperateurConnu operateur);
  }

  interface SyntheseDesHeuresSemaineBuilder {
    SyntheseDesHeuresJoursBuilder semaine(SemaineCalendaire semaine);
  }

  interface SyntheseDesHeuresJoursBuilder {
    SyntheseDesHeuresElementsBuilder jours(List<JourDeSynthese> jours);
  }

  interface SyntheseDesHeuresElementsBuilder {
    SyntheseDesHeures elements(List<ElementDeLaSynthese> elements);
  }
}
