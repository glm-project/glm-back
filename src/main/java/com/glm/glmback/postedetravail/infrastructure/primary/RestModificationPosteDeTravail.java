package com.glm.glmback.postedetravail.infrastructure.primary;

import com.glm.glmback.postedetravail.domain.PosteDeTravailAModifier;
import com.glm.glmback.postedetravail.domain.PosteDeTravailId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Revision d'un poste de travail.")
record RestModificationPosteDeTravail(
  @Schema(description = "Nom du poste, unique dans l'entreprise.", example = "Tour 1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  @Size(max = 100)
  String libelle,

  @Schema(description = "Metier qui s'exerce sur ce poste.", example = "tournage", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  @Size(max = 50)
  String nature
) {
  PosteDeTravailAModifier toDomain(PosteDeTravailId id) {
    return new PosteDeTravailAModifier(id, libelle, nature);
  }
}
