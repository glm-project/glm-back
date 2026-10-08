package com.glm.glmback.categoriedeproduit.infrastructure.primary;

import com.glm.glmback.categoriedeproduit.domain.CategorieDeProduit;
import com.glm.glmback.categoriedeproduit.domain.CategorieDeProduitListee;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Une categorie de produit de l'entreprise.")
record RestCategorieDeProduit(
  @Schema(
    description = "Code de la categorie, affiche tel quel et prefixe du nom des produits.",
    example = "MOULE",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  String code,
  @Schema(
    description = "Des produits sont ranges dans la categorie : elle ne peut pas etre supprimee.",
    example = "true",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  boolean utilisee
) {
  static RestCategorieDeProduit from(CategorieDeProduitListee listee) {
    return new RestCategorieDeProduit(listee.categorie().code().value(), listee.utilisee());
  }

  static RestCategorieDeProduit declaree(CategorieDeProduit categorie) {
    return new RestCategorieDeProduit(categorie.code().value(), false);
  }
}
