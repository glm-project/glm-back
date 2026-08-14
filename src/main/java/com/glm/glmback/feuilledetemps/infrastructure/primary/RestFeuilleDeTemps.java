package com.glm.glmback.feuilledetemps.infrastructure.primary;

import com.glm.glmback.feuilledetemps.domain.FeuilleDeTemps;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
  description = """
  L'historique calendaire d'un operateur sur une semaine ISO.

  Rien n'est stocke : la feuille est recalculee a chaque lecture depuis les journaux de l'atelier, pour qu'une saisie
  regularisee apres coup compte a l'heure ou le travail a eu lieu.
  """
)
record RestFeuilleDeTemps(
  @Schema(description = "L'operateur, resolu au referentiel.") RestOperateur operateur,
  @Schema(description = "Annee ISO de la semaine. Attention, elle differe de l'annee civile aux changements d'annee.", example = "2026")
  int annee,
  @Schema(description = "Numero de la semaine ISO.", example = "20") int semaine,
  @Schema(description = "Les sept jours, du lundi au dimanche.") List<RestJourDeLaSemaine> jours
) {
  static RestFeuilleDeTemps from(FeuilleDeTemps feuille) {
    return new RestFeuilleDeTemps(
      RestOperateur.from(feuille.operateur()),
      feuille.semaine().annee(),
      feuille.semaine().numero(),
      feuille.jours().stream().map(RestJourDeLaSemaine::from).toList()
    );
  }
}
