package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.ElementValorise;
import com.glm.glmback.coutderevient.domain.TypeDElement;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "L'element de fabrication dont le cout de revient est lu, relu au referentiel a chaque appel.")
record RestElement(
  @Schema(description = "Identifiant de l'element de fabrication.") UUID id,
  @Schema(description = "Nom de l'element, produit par la numerotation automatique.", example = "OF-2026-000001") String nom,
  @Schema(description = "Type de l'element.") TypeDElement type
) {
  static RestElement from(ElementValorise element) {
    return new RestElement(element.element().uuid(), element.nom().value(), element.type());
  }
}
