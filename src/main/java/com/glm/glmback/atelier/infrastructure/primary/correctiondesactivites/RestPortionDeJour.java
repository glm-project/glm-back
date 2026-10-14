package com.glm.glmback.atelier.infrastructure.primary.correctiondesactivites;

import com.glm.glmback.atelier.domain.correctiondesactivites.PortionDeJour;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.LocalDate;

@Schema(description = "La part d'une activite comptee un autre jour, coupee aux minuits de l'entreprise.")
record RestPortionDeJour(
  @Schema(description = "Le jour calendaire de l'entreprise.", example = "2026-10-06", requiredMode = Schema.RequiredMode.REQUIRED)
  LocalDate jour,
  @Schema(
    description = "La duree comptee ce jour-la, ISO-8601 : un ecart reel entre deux instants.",
    type = "string",
    example = "PT3H30M",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Duration duree
) {
  static RestPortionDeJour from(PortionDeJour portion) {
    return new RestPortionDeJour(portion.jour(), portion.duree());
  }
}
