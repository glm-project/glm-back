package com.glm.glmback.atelier.infrastructure.primary.correctiondesactivites;

import com.glm.glmback.atelier.domain.PosteConnu;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Un poste de travail du referentiel avec sa nature d'operation courante, relus a chaque lecture.")
record RestPosteDeLaJournee(
  @Schema(description = "Identifiant du poste dans le referentiel.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Libelle du poste.", example = "Tour CN 1", requiredMode = Schema.RequiredMode.REQUIRED) String libelle,
  @Schema(
    description = "Nature de l'operation qui s'exerce sur le poste.",
    example = "Tournage",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  String nature
) {
  static RestPosteDeLaJournee from(PosteConnu poste) {
    return new RestPosteDeLaJournee(poste.id().uuid(), poste.libelle().value(), poste.nature().libelle());
  }
}
