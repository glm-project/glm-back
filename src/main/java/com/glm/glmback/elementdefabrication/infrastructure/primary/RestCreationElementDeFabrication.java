package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationToCreate;
import com.glm.glmback.elementdefabrication.domain.TypeDElementDeFabrication;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record RestCreationElementDeFabrication(
  @NotNull TypeDElementDeFabrication type,
  @Size(max = 100) String reference,
  @Size(max = 1000) String description
) {
  ElementDeFabricationToCreate toDomain() {
    return new ElementDeFabricationToCreate(type, reference, description);
  }
}
