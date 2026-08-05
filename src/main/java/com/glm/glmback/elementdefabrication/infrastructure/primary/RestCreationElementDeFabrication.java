package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationToCreate;
import com.glm.glmback.elementdefabrication.domain.TypeDElementDeFabrication;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record RestCreationElementDeFabrication(
  @NotNull TypeDElementDeFabrication type,
  @NotBlank @Size(max = 255) String titre,
  @NotBlank @Size(max = 1000) String description
) {
  ElementDeFabricationToCreate toDomain() {
    return new ElementDeFabricationToCreate(type, titre, description);
  }
}
