package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.DureeTotale;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;

@Schema(description = "Duree totale.")
record RestDureeDuCout(
  @Schema(description = "Duree ISO-8601.", example = "PT2H", requiredMode = Schema.RequiredMode.REQUIRED) Duration valeur
) {
  static RestDureeDuCout from(DureeTotale total) {
    return new RestDureeDuCout(total.valeur());
  }
}
