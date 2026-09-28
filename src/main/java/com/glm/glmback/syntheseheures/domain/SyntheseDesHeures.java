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

  /**
   * La duree travaillee de la semaine, somme des sept jours.
   */
  public Duration dureeTotale() {
    return jours.stream().map(JourDeSynthese::duree).reduce(Duration.ZERO, Duration::plus);
  }

  /**
   * La duree presumee de la semaine : ce qui reste a confirmer par une regularisation du depart.
   */
  public Duration dureePresumeeTotale() {
    return jours.stream().map(JourDeSynthese::dureePresumee).reduce(Duration.ZERO, Duration::plus);
  }

  /**
   * Le temps operationnel pointe de la semaine, somme des sept jours. Il se cumule par element, et peut donc depasser
   * la presence.
   */
  public Duration dureeOperationnelleTotale() {
    return jours.stream().map(JourDeSynthese::dureeOperationnelle).reduce(Duration.ZERO, Duration::plus);
  }

  public Duration dureeOperationnellePresumeeTotale() {
    return jours.stream().map(JourDeSynthese::dureeOperationnellePresumee).reduce(Duration.ZERO, Duration::plus);
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
