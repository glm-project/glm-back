package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.SyntheseDesHeures;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.util.List;

@Schema(
  description = "Temps operationnel d'un operateur sur une semaine ISO, recalcule depuis les activites interpretees par atelier. Aucun montant n'est calcule."
)
record RestSyntheseDesHeures(
  @Schema(description = "Operateur resolu au referentiel.") RestOperateur operateur,
  @Schema(description = "Annee des semaines ISO.", example = "2026") int annee,
  @Schema(description = "Numero de la semaine ISO.", example = "20") int semaine,
  @Schema(description = "Les sept jours du lundi au dimanche, vides compris.") List<RestJourDeSynthese> jours,
  @Schema(
    description = "Somme des durees des sept jours et des elements. Une activite encore en cours ne compte rien.",
    example = "PT13H",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Duration dureeOperationnelleTotale,
  @Schema(
    description = "Elements portant une activite ou un pointage dans la semaine, par premiere apparition puis nom. Un element reengage reste un seul element.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<RestElementDeLaSynthese> elements
) {
  static RestSyntheseDesHeures from(SyntheseDesHeures synthese) {
    return new RestSyntheseDesHeures(
      RestOperateur.from(synthese.operateur()),
      synthese.semaine().annee(),
      synthese.semaine().numero(),
      synthese.jours().stream().map(RestJourDeSynthese::from).toList(),
      synthese.dureeOperationnelleTotale(),
      synthese.elements().stream().map(RestElementDeLaSynthese::from).toList()
    );
  }
}
