package com.glm.glmback.atelier.infrastructure.primary.correctiondesactivites;

import com.glm.glmback.atelier.domain.correctiondesactivites.JourneeACorriger;
import com.glm.glmback.atelier.domain.correctiondesactivites.SuiviPresent;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  description = """
  Un suivi present a l'atelier le jour lu : engage avant la fin du jour et non cloture avant son debut, y compris cloture
  depuis. C'est le suivi qu'une correction vise, et un choix pour ajouter une activite.
  """
)
record RestSuiviPresent(
  @Schema(description = "Identifiant du suivi : celui des routes de correction.", requiredMode = Schema.RequiredMode.REQUIRED) UUID suivi,
  @Schema(description = "L'element engage dans ce suivi.", requiredMode = Schema.RequiredMode.REQUIRED) RestElementDeLaJournee element,
  @Schema(
    description = "Instant de l'engagement de l'element dans l'atelier : aucun evenement ne le precede.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Instant engageLe,
  @Schema(description = "Instant de la cloture du suivi, absent s'il est ouvert : aucun evenement ne la suit.") Instant clotureLe
) {
  static RestSuiviPresent from(SuiviPresent suivi, JourneeACorriger journee) {
    return new RestSuiviPresent(
      suivi.id().uuid(),
      RestElementDeLaJournee.from(suivi.element(), journee.fiche(suivi.element().id())),
      suivi.engageLe(),
      suivi.clotureLe().orElse(null)
    );
  }
}
