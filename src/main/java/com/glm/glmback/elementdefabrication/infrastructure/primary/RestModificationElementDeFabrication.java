package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationToUpdate;
import jakarta.validation.constraints.Size;

record RestModificationElementDeFabrication(@Size(max = 100) String reference, @Size(max = 1000) String description) {
  ElementDeFabricationToUpdate toDomain(ElementDeFabricationId id) {
    return new ElementDeFabricationToUpdate(id, reference, description);
  }
}
