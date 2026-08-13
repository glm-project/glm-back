package com.glm.glmback.operateur.infrastructure.primary;

import com.glm.glmback.operateur.domain.PosteHabilitable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Un poste sur lequel l'operateur est habilite, resolu au moment de la lecture.")
record RestPosteHabilite(
  @Schema(description = "Identifiant du poste.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,

  @Schema(description = "Nom du poste.", example = "Tour 1", requiredMode = Schema.RequiredMode.REQUIRED) String libelle,

  @Schema(description = "Metier qui s'exerce sur ce poste.", example = "tournage", requiredMode = Schema.RequiredMode.REQUIRED)
  String nature
) {
  static RestPosteHabilite from(PosteHabilitable poste) {
    return new RestPosteHabilite(poste.id().uuid(), poste.libelle().value(), poste.nature().value());
  }
}
