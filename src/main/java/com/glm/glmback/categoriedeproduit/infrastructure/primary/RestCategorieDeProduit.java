package com.glm.glmback.categoriedeproduit.infrastructure.primary;

import com.glm.glmback.categoriedeproduit.domain.CategorieDeProduit;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Une categorie de produit de l'entreprise.")
record RestCategorieDeProduit(
  @Schema(
    description = "Code de la categorie, affiche tel quel et prefixe du nom des produits.",
    example = "MOULE",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  String code
) {
  static RestCategorieDeProduit from(CategorieDeProduit categorie) {
    return new RestCategorieDeProduit(categorie.code().value());
  }
}
