package com.glm.glmback.parametrage.infrastructure.primary;

import com.glm.glmback.parametrage.domain.Modification;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "La derniere modification du parametrage.")
record RestModification(
  @Schema(
    description = "Utilisateur qui l'a faite, lu dans son jeton.",
    example = "gestionnaire",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  String auteur,

  @Schema(description = "Instant de la modification.", requiredMode = Schema.RequiredMode.REQUIRED) Instant date
) {
  static RestModification from(Modification modification) {
    return new RestModification(modification.auteur().value(), modification.date());
  }
}
