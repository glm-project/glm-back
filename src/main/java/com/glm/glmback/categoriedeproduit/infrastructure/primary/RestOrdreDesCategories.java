package com.glm.glmback.categoriedeproduit.infrastructure.primary;

import com.glm.glmback.categoriedeproduit.domain.CodeDeCategorie;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

@Schema(description = "Ordre d'affichage des categories de produit, de la premiere a la derniere.")
record RestOrdreDesCategories(
  @Schema(
    description = "Codes de toutes les categories de l'entreprise, chacun une fois, dans l'ordre voulu.",
    example = "[\"OF\", \"MOULE\"]",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  List<@NotBlank @Pattern(regexp = "^[A-Z]{1,10}$") String> codes
) {
  List<CodeDeCategorie> toDomain() {
    return codes.stream().map(CodeDeCategorie::new).toList();
  }
}
