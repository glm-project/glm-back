package com.glm.glmback.parametrage.infrastructure.primary;

import com.glm.glmback.parametrage.domain.Parametrage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;

@Schema(description = "Les reglages de l'entreprise.")
record RestParametrage(
  @Schema(
    description = "Duree au bout de laquelle une activite que rien n'a terminee se termine automatiquement, en ISO-8601. Treize heures tant que l'entreprise ne l'a pas fixee.",
    example = "PT13H",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Duration dureeMaxDActivite
) {
  static RestParametrage from(Parametrage parametrage) {
    return new RestParametrage(parametrage.dureeMaxDActivite().value());
  }
}
