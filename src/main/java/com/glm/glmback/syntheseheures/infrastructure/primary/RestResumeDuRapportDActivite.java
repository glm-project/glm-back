package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.RapportDActivite;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

@Schema(
  description = """
  Le resume d'un rapport d'activite, affiche avant de le telecharger : le temps pointe de la periode et ce qui
  merite l'attention. Aucun montant.
  """
)
record RestResumeDuRapportDActivite(
  @Schema(description = "Operateur resolu au referentiel.", requiredMode = Schema.RequiredMode.REQUIRED) RestOperateur operateur,
  @Schema(description = "Premier jour de la periode.", example = "2026-09-01", requiredMode = Schema.RequiredMode.REQUIRED)
  LocalDate premierJour,
  @Schema(description = "Dernier jour de la periode, inclus.", example = "2026-09-30", requiredMode = Schema.RequiredMode.REQUIRED)
  LocalDate dernierJour,
  @Schema(description = "Instant de lecture du rapport.", requiredMode = Schema.RequiredMode.REQUIRED) Instant evaluation,
  @Schema(
    description = "Temps pointe de la periode : une heure sur deux elements compte sur chacun ; une activite en cours ne compte rien.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  RestDureeDeSynthese tempsPointe,
  @Schema(description = "Part du temps pointe passee en non-conformite.", requiredMode = Schema.RequiredMode.REQUIRED)
  RestDureeDeSynthese dureeNonConformite,
  @Schema(description = "Jours portant au moins un pointage.", example = "17", requiredMode = Schema.RequiredMode.REQUIRED)
  long joursAvecPointage,
  @Schema(
    description = "Pointages du journal ; une activite qui passe minuit en compte un par jour.",
    example = "94",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  long pointages,
  @Schema(
    description = "Pointages termines automatiquement a leur echeance, a regulariser ; ils n'empechent pas l'export.",
    example = "1",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  long finsAutomatiques
) {
  static RestResumeDuRapportDActivite from(RapportDActivite rapport) {
    return new RestResumeDuRapportDActivite(
      RestOperateur.from(rapport.operateur()),
      rapport.periode().premierJour(),
      rapport.periode().dernierJour(),
      rapport.evaluation(),
      RestDureeDeSynthese.from(rapport.tempsPointe()),
      RestDureeDeSynthese.from(rapport.dureeNonConformite()),
      rapport.joursAvecPointage(),
      rapport.journal().size(),
      rapport.finsAutomatiques()
    );
  }
}
