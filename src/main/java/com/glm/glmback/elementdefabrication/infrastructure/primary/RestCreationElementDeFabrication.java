package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationToCreate;
import com.glm.glmback.shared.elementtype.infrastructure.primary.LegacyElementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record RestCreationElementDeFabrication(
  @Schema(
    description = "Code de la categorie de produit, declaree par l'entreprise. Obligatoire, sauf pour un client qui envoie encore type.",
    example = "MOULE"
  )
  @Pattern(regexp = "^[A-Z]{1,10}$")
  String categorie,

  @Schema(
    description = "Remplace par categorie. Lu seulement en l'absence de categorie : ORDRE_DE_FABRICATION vaut OF, PRODUIT vaut MOULE.",
    allowableValues = { LegacyElementType.ORDRE_DE_FABRICATION, LegacyElementType.PRODUIT },
    deprecated = true
  )
  @Pattern(regexp = LegacyElementType.ORDRE_DE_FABRICATION + "|" + LegacyElementType.PRODUIT)
  String type,

  @Size(max = 100) String reference,
  @Size(max = 1000) String description
) {
  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = "la categorie est obligatoire")
  boolean isCategorieRenseignee() {
    return categorie != null || type != null;
  }

  ElementDeFabricationToCreate toDomain() {
    return new ElementDeFabricationToCreate(categorieDemandee(), reference, description);
  }

  private String categorieDemandee() {
    return categorie != null ? categorie : LegacyElementType.toCategory(type).orElseThrow();
  }
}
