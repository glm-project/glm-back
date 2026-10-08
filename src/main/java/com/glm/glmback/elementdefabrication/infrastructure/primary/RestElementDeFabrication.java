package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.Description;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.Reference;
import com.glm.glmback.shared.elementtype.infrastructure.primary.LegacyElementType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

record RestElementDeFabrication(
  String categorie,
  @Schema(
    description = "Remplace par categorie : ORDRE_DE_FABRICATION pour la categorie OF, PRODUIT pour toute autre.",
    allowableValues = { LegacyElementType.ORDRE_DE_FABRICATION, LegacyElementType.PRODUIT },
    deprecated = true
  )
  String type,
  UUID id,
  String nom,
  String reference,
  String description,
  Instant dateDeCreation,
  Instant dateDeModification
) {
  static RestElementDeFabrication from(ElementDeFabrication element) {
    return new RestElementDeFabrication(
      element.categorie().value(),
      LegacyElementType.fromCategory(element.categorie().value()),
      element.id().uuid(),
      element.nom().value(),
      element.reference().map(Reference::value).orElse(null),
      element.description().map(Description::value).orElse(null),
      element.dateDeCreation(),
      element.dateDeModification()
    );
  }
}
