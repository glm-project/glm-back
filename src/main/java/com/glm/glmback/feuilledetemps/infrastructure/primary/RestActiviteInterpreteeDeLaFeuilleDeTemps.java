package com.glm.glmback.feuilledetemps.infrastructure.primary;

import com.glm.glmback.feuilledetemps.domain.ActiviteLue;
import com.glm.glmback.feuilledetemps.domain.EtatDActivite;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "L'activite entiere interpretee par atelier, avant son decoupage calendaire.")
record RestActiviteInterpreteeDeLaFeuilleDeTemps(
  @Schema(description = "Identite stable de l'activite, conservee apres correction.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "TERMINEE_AUTOMATIQUEMENT signale l'anomalie de fin automatique.", requiredMode = Schema.RequiredMode.REQUIRED)
  EtatDActivite etat,
  @Schema(description = "Debut de l'activite entiere.", requiredMode = Schema.RequiredMode.REQUIRED) Instant debut,
  @Schema(description = "Fin de l'activite entiere, seulement si elle est terminee ou terminee automatiquement.") Instant fin
) {
  static RestActiviteInterpreteeDeLaFeuilleDeTemps from(ActiviteLue activite) {
    return new RestActiviteInterpreteeDeLaFeuilleDeTemps(
      activite.id().uuid(),
      activite.etat(),
      activite.plage().debut(),
      activite.plage().fin().orElse(null)
    );
  }
}
