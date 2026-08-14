package com.glm.glmback.feuilledetemps.infrastructure.primary;

import com.glm.glmback.feuilledetemps.domain.Plage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(
  description = """
  Un intervalle de presence, deja ramene au jour qui le porte.

  Une plage sans fin n'est pas une anomalie : l'operateur n'a pas encore pointe son depart.
  """
)
record RestPlage(
  @Schema(description = "Debut de la plage.", example = "2026-05-11T06:00:00Z") Instant debut,
  @Schema(description = "Fin de la plage, absente tant que l'operateur n'est pas parti.") Instant fin
) {
  static RestPlage from(Plage plage) {
    return new RestPlage(plage.debut(), plage.fin().orElse(null));
  }
}
