package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.DureeTotale;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;

@Schema(description = "Duree totale.")
record RestDureeDeSynthese(
  @Schema(description = "Duree ISO-8601.", example = "PT2H", requiredMode = Schema.RequiredMode.REQUIRED) Duration valeur
) {
  static RestDureeDeSynthese from(DureeTotale duree) {
    return new RestDureeDeSynthese(duree.valeur());
  }
}
