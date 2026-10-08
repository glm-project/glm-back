package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.AnnuaireDuCout;
import com.glm.glmback.coutderevient.domain.ElementId;
import com.glm.glmback.coutderevient.domain.ElementValorise;
import com.glm.glmback.shared.elementtype.infrastructure.primary.LegacyElementType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import java.util.UUID;

@Schema(
  name = "RestElementCiteDuCout",
  description = "L'element sur lequel portait une activite citee par le detail, nomme a la lecture ; nom et categorie absents s'il est inconnu."
)
record RestElementCite(
  @Schema(description = "Identifiant de l'element de fabrication.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Nom de l'element.", example = "OF-2026-000002") String nom,
  @Schema(description = "Categorie de l'element.") String categorie,
  @Schema(
    description = "Remplace par categorie : ORDRE_DE_FABRICATION pour la categorie OF, PRODUIT pour toute autre.",
    allowableValues = { LegacyElementType.ORDRE_DE_FABRICATION, LegacyElementType.PRODUIT },
    deprecated = true
  )
  String type
) {
  static RestElementCite from(ElementId element, AnnuaireDuCout annuaire) {
    Optional<ElementValorise> valorise = annuaire.element(element);
    return new RestElementCite(
      element.uuid(),
      valorise.map(connu -> connu.nom().value()).orElse(null),
      valorise.map(connu -> connu.categorie().value()).orElse(null),
      valorise.map(connu -> LegacyElementType.fromCategory(connu.categorie().value())).orElse(null)
    );
  }
}
