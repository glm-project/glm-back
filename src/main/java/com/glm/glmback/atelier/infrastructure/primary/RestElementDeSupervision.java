package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.ElementDeSupervision;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

record RestElementDeSupervision(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Categorie de l'element, copiee a l'engagement.", requiredMode = Schema.RequiredMode.REQUIRED) String categorie,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nom,
  String reference
) {
  static RestElementDeSupervision from(ElementDeSupervision element) {
    return new RestElementDeSupervision(
      element.element().id().uuid(),
      element.element().categorie().value(),
      element.element().nom().value(),
      element.reference().orElse(null)
    );
  }
}
