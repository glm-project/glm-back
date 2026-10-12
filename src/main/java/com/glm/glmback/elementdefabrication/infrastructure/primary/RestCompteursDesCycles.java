package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.CompteursDesCycles;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Le nombre d'elements de chaque cycle, pour les memes filtres.")
record RestCompteursDesCycles(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long enPreparation,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long enAtelier,
  @Schema(description = "Sur la periode de cloture quand elle est donnee.", requiredMode = Schema.RequiredMode.REQUIRED) long clotures
) {
  static RestCompteursDesCycles from(CompteursDesCycles compteurs) {
    return new RestCompteursDesCycles(compteurs.enPreparation(), compteurs.enAtelier(), compteurs.clotures());
  }
}
