package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.ElementValorise;
import com.glm.glmback.shared.elementtype.infrastructure.primary.LegacyElementType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "L'element de fabrication dont le cout de revient est lu, relu au referentiel a chaque appel.")
record RestElement(
  @Schema(description = "Identifiant de l'element de fabrication.") UUID id,
  @Schema(description = "Nom de l'element, produit par la numerotation automatique.", example = "OF-2026-000001") String nom,
  @Schema(description = "Categorie de l'element.") String categorie,
  @Schema(
    description = "Remplace par categorie : ORDRE_DE_FABRICATION pour la categorie OF, PRODUIT pour toute autre.",
    allowableValues = { LegacyElementType.ORDRE_DE_FABRICATION, LegacyElementType.PRODUIT },
    deprecated = true
  )
  String type
) {
  static RestElement from(ElementValorise element) {
    return new RestElement(
      element.element().uuid(),
      element.nom().value(),
      element.categorie().value(),
      LegacyElementType.fromCategory(element.categorie().value())
    );
  }
}
