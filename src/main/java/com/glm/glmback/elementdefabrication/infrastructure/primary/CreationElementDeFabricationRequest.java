package com.glm.glmback.elementdefabrication.infrastructure.primary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreationElementDeFabricationRequest(
  String nom,
  @NotBlank @Size(max = 255) String titre,
  @NotBlank @Size(max = 1000) String description
) {}
