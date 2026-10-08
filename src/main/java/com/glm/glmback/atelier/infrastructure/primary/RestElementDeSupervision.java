package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.ElementDeSupervision;
import com.glm.glmback.shared.elementtype.infrastructure.primary.LegacyElementType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

record RestElementDeSupervision(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Categorie de l'element, copiee a l'engagement.", requiredMode = Schema.RequiredMode.REQUIRED) String categorie,
  @Schema(
    description = "Remplace par categorie : ORDRE_DE_FABRICATION pour la categorie OF, PRODUIT pour toute autre.",
    allowableValues = { LegacyElementType.ORDRE_DE_FABRICATION, LegacyElementType.PRODUIT },
    deprecated = true,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  String type,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nom,
  String reference
) {
  static RestElementDeSupervision from(ElementDeSupervision element) {
    return new RestElementDeSupervision(
      element.element().id().uuid(),
      element.element().categorie().value(),
      LegacyElementType.fromCategory(element.element().categorie().value()),
      element.element().nom().value(),
      element.reference().orElse(null)
    );
  }
}
