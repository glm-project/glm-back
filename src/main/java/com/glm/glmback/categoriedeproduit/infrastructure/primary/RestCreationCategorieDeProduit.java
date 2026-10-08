package com.glm.glmback.categoriedeproduit.infrastructure.primary;

import com.glm.glmback.categoriedeproduit.domain.CodeDeCategorie;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Declaration d'une categorie de produit.")
record RestCreationCategorieDeProduit(
  @Schema(
    description = "Code de la categorie, unique dans l'entreprise : de 1 a 10 lettres majuscules, sans accent. Il sert de libelle et de prefixe au nom des produits, et ne pourra plus etre modifie.",
    example = "MOULE",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotBlank
  @Pattern(regexp = "^[A-Z]{1,10}$")
  String code
) {
  CodeDeCategorie toDomain() {
    return new CodeDeCategorie(code);
  }
}
