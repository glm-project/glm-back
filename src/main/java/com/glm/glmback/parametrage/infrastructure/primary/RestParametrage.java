package com.glm.glmback.parametrage.infrastructure.primary;

import com.glm.glmback.parametrage.domain.Parametrage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;

@Schema(description = "Le parametrage de l'entreprise.")
record RestParametrage(
  @Schema(
    description = "Au-dela de cette duree depuis l'arrivee, une journee de travail sans depart est abandonnee. Duree ISO 8601.",
    example = "PT13H",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Duration amplitudeMaximale,

  @Schema(description = "Absente tant que le parametrage n'a jamais ete modifie depuis sa creation.") RestModification derniereModification
) {
  static RestParametrage from(Parametrage parametrage) {
    return new RestParametrage(
      parametrage.amplitudeMaximale().value(),
      parametrage.derniereModification().map(RestModification::from).orElse(null)
    );
  }
}
