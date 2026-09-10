package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.SyntheseDesHeures;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.util.List;

@Schema(
  description = """
  Le releve des heures d'un operateur sur une semaine ISO.

  Rien n'est stocke : le releve est recalcule a chaque lecture depuis les journaux de l'atelier, pour qu'une saisie
  regularisee apres coup compte a l'heure ou le travail a eu lieu. Ce n'est ni une feuille de paie ni un rapport de
  paie : il expose du temps travaille, il ne calcule aucun montant.
  """
)
record RestSyntheseDesHeures(
  @Schema(description = "L'operateur, resolu au referentiel.") RestOperateur operateur,
  @Schema(description = "Annee ISO de la semaine. Attention, elle differe de l'annee civile aux changements d'annee.", example = "2026")
  int annee,
  @Schema(description = "Numero de la semaine ISO.", example = "20") int semaine,
  @Schema(description = "Les sept jours, du lundi au dimanche.") List<RestJourDeSynthese> jours,
  @Schema(description = "Duree travaillee de la semaine, somme des sept jours.", example = "PT38H") Duration dureeTotale,
  @Schema(description = "Vrai des qu'un jour de la semaine porte une anomalie.") boolean aUneAnomalie
) {
  static RestSyntheseDesHeures from(SyntheseDesHeures synthese) {
    return new RestSyntheseDesHeures(
      RestOperateur.from(synthese.operateur()),
      synthese.semaine().annee(),
      synthese.semaine().numero(),
      synthese.jours().stream().map(RestJourDeSynthese::from).toList(),
      synthese.dureeTotale(),
      synthese.aUneAnomalie()
    );
  }
}
