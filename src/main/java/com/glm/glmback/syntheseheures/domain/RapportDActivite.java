package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * Toute l'activite d'un operateur sur une semaine ou un mois : ses jours, les elements et les natures de travail
 * qu'il a pointes, et le journal de ses pointages.
 *
 * <p>
 * Memes regles que la synthese des heures : une heure passee sur deux elements compte sur chacun, si bien que la
 * somme des jours, celle des elements, celle des natures et celle du journal sont toutes le temps pointe. Aucun
 * montant, et une fin automatique n'empeche rien : elle est comptee jusqu'a son echeance et signalee.
 * </p>
 */
public record RapportDActivite(
  OperateurConnu operateur,
  PeriodeCalendaire periode,
  Instant evaluation,
  ZoneId fuseau,
  List<JourDuRapport> jours,
  List<ElementDeLaSynthese> elements,
  List<NatureDuRapport> natures
) {
  public RapportDActivite {
    Assert.notNull("operateur", operateur);
    Assert.notNull("periode", periode);
    Assert.notNull("evaluation", evaluation);
    Assert.notNull("fuseau horaire", fuseau);
    Assert.field("jours", jours).notNull().noNullElement();
    Assert.field("elements", elements).notNull().noNullElement();
    Assert.field("natures", natures).notNull().noNullElement();
  }

  static RapportOperateurBuilder builder() {
    return operateur ->
      periode ->
        evaluation ->
          fuseau -> jours -> elements -> natures -> new RapportDActivite(operateur, periode, evaluation, fuseau, jours, elements, natures);
  }

  public DureeTotale tempsPointe() {
    return DureeTotale.somme(jours.stream().map(JourDuRapport::duree).toList());
  }

  public DureeTotale dureeNonConformite() {
    return DureeTotale.somme(jours.stream().map(JourDuRapport::dureeNonConformite).toList());
  }

  public List<LigneDuJournal> journal() {
    return jours
      .stream()
      .flatMap(jour -> jour.lignes().stream())
      .toList();
  }

  public long joursAvecPointage() {
    return jours
      .stream()
      .filter(jour -> !jour.lignes().isEmpty())
      .count();
  }

  public long finsAutomatiques() {
    return journal()
      .stream()
      .filter(ligne -> ligne.etat() == EtatDActivite.TERMINEE_AUTOMATIQUEMENT)
      .count();
  }

  interface RapportOperateurBuilder {
    RapportPeriodeBuilder operateur(OperateurConnu operateur);
  }

  interface RapportPeriodeBuilder {
    RapportEvaluationBuilder periode(PeriodeCalendaire periode);
  }

  interface RapportEvaluationBuilder {
    RapportFuseauBuilder evaluation(Instant evaluation);
  }

  interface RapportFuseauBuilder {
    RapportJoursBuilder fuseau(ZoneId fuseau);
  }

  interface RapportJoursBuilder {
    RapportElementsBuilder jours(List<JourDuRapport> jours);
  }

  interface RapportElementsBuilder {
    RapportNaturesBuilder elements(List<ElementDeLaSynthese> elements);
  }

  interface RapportNaturesBuilder {
    RapportDActivite natures(List<NatureDuRapport> natures);
  }
}
