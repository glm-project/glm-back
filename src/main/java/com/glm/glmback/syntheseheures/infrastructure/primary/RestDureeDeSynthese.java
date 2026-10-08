package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.DureeTotale;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;

@Schema(description = "Duree totale certaine.")
record RestDureeDeSynthese(
  @Schema(description = "Duree certaine.", example = "PT2H", requiredMode = Schema.RequiredMode.REQUIRED) Duration valeur,
  @Schema(
    description = "Toujours vrai : aucune activite a resoudre ne contribue plus a un total. Conserve le temps que le front cesse de le lire.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  boolean complete
) {
  static RestDureeDeSynthese from(DureeTotale duree) {
    return new RestDureeDeSynthese(duree.valeur(), true);
  }
}
