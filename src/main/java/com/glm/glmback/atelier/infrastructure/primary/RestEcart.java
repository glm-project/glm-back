package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.MotifDEcart;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RestEcart(
  @Schema(description = "Pourquoi le geste est ecarte.", example = "Badge d'un visiteur", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  @Size(max = 255)
  String motif
) {
  MotifDEcart toDomain() {
    return new MotifDEcart(motif);
  }
}
