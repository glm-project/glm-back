package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.ElementDeSupervision;
import com.glm.glmback.atelier.domain.TypeDElementEngage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

record RestElementDeSupervision(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TypeDElementEngage type,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nom,
  String reference
) {
  static RestElementDeSupervision from(ElementDeSupervision element) {
    return new RestElementDeSupervision(
      element.element().id().uuid(),
      element.element().type(),
      element.element().nom().value(),
      element.reference().orElse(null)
    );
  }
}
