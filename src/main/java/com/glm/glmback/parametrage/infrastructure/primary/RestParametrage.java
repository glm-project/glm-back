package com.glm.glmback.parametrage.infrastructure.primary;

import com.glm.glmback.parametrage.application.ParametrageLu;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;

@Schema(description = "Les reglages de l'entreprise.")
record RestParametrage(
  @Schema(
    description = "Duree au bout de laquelle une activite que rien n'a terminee se termine automatiquement, en ISO-8601. Treize heures tant que l'entreprise ne l'a pas fixee.",
    example = "PT13H",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Duration dureeMaxDActivite,
  @Schema(
    description = "Le logo de l'entreprise, absent tant qu'aucun n'est depose. Son image se lit a l'adresse /api/parametrage/logo/{version}."
  )
  RestLogo logo
) {
  static RestParametrage from(ParametrageLu lu) {
    return new RestParametrage(lu.parametrage().dureeMaxDActivite().value(), lu.versionDuLogo().map(RestLogo::from).orElse(null));
  }
}
