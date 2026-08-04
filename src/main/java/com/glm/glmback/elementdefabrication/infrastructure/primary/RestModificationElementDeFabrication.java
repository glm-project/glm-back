package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.Description;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationToUpdate;
import com.glm.glmback.elementdefabrication.domain.Titre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RestModificationElementDeFabrication(@NotBlank @Size(max = 255) String titre, @NotBlank @Size(max = 1000) String description) {
  ElementDeFabricationToUpdate toDomain(ElementDeFabricationId id) {
    return new ElementDeFabricationToUpdate(id, new Titre(titre), new Description(description));
  }
}
