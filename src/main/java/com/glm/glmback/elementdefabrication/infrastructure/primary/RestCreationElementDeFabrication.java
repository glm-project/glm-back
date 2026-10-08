package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationToCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record RestCreationElementDeFabrication(
  @Schema(
    description = "Code de la categorie de produit, declaree par l'entreprise.",
    example = "MOULE",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotBlank
  @Pattern(regexp = "^[A-Z]{1,10}$")
  String categorie,

  @Size(max = 100) String reference,
  @Size(max = 1000) String description
) {
  ElementDeFabricationToCreate toDomain() {
    return new ElementDeFabricationToCreate(categorie, reference, description);
  }
}
