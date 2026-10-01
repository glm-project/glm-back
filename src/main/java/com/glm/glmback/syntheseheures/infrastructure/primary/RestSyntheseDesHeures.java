package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.SyntheseDesHeures;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(
  description = "Temps operationnel d'un operateur sur une semaine ISO, recalcule depuis les activites interpretees par atelier. Aucun montant n'est calcule."
)
record RestSyntheseDesHeures(
  @Schema(
    description = "Instant effectivement utilise pour l'expiration et le decoupage des activites en cours.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Instant evaluation,
  @Schema(description = "Operateur resolu au referentiel.") RestOperateur operateur,
  @Schema(description = "Annee des semaines ISO.", example = "2026") int annee,
  @Schema(description = "Numero de la semaine ISO.", example = "20") int semaine,
  @Schema(description = "Les sept jours du lundi au dimanche, vides compris.") List<RestJourDeSynthese> jours,
  @Schema(
    description = "Total de travail et NC des sept jours. Incomplet sans valeur si une activite a resoudre y contribue ; une activite en cours ne compte rien.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  RestDureeDeSynthese dureeOperationnelleTotale,
  @Schema(
    description = "Elements portant une activite ou un pointage dans la semaine, par premiere apparition puis nom. Un element reengage reste un seul element.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<RestElementDeLaSynthese> elements,
  @Schema(
    description = "Conflits concernant une activite ou un pointage rendu dans cette semaine.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<RestConflitDeSynthese> conflits
) {
  static RestSyntheseDesHeures from(SyntheseDesHeures synthese) {
    return new RestSyntheseDesHeures(
      synthese.evaluation(),
      RestOperateur.from(synthese.operateur()),
      synthese.semaine().annee(),
      synthese.semaine().numero(),
      synthese.jours().stream().map(RestJourDeSynthese::from).toList(),
      RestDureeDeSynthese.from(synthese.dureeOperationnelleTotale()),
      synthese.elements().stream().map(RestElementDeLaSynthese::from).toList(),
      synthese.conflits().stream().map(RestConflitDeSynthese::from).toList()
    );
  }
}
