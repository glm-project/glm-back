package com.glm.glmback.postedetravail.infrastructure.primary;

import com.glm.glmback.postedetravail.domain.PosteDeTravail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Un poste de travail du referentiel de l'entreprise.")
record RestPosteDeTravail(
  @Schema(description = "Identifiant du poste.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,

  @Schema(description = "Nom du poste tel que l'atelier le designe.", example = "Tour 1", requiredMode = Schema.RequiredMode.REQUIRED)
  String libelle,

  @Schema(description = "Metier qui s'exerce sur ce poste.", example = "tournage", requiredMode = Schema.RequiredMode.REQUIRED)
  String nature
) {
  static RestPosteDeTravail from(PosteDeTravail poste) {
    return new RestPosteDeTravail(poste.id().uuid(), poste.libelle().value(), poste.nature().value());
  }
}
