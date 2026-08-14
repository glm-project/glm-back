package com.glm.glmback.postedetravail.infrastructure.primary;

import com.glm.glmback.postedetravail.domain.PosteDeTravailACreer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Declaration d'un poste de travail.")
record RestCreationPosteDeTravail(
  @Schema(description = "Nom du poste, unique dans l'entreprise.", example = "Tour 1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  @Size(max = 100)
  String libelle,

  @Schema(
    description = "Metier qui s'exerce sur ce poste. Obligatoire : un poste est declare pour dire quel travail s'y fait.",
    example = "tournage",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotBlank
  @Size(max = 50)
  String nature,

  @Schema(
    description = "Cout horaire du poste, destine au cout de revient. Facultatif : toutes les entreprises ne le valorisent pas.",
    example = "45.50"
  )
  @DecimalMin(value = "0", inclusive = false)
  BigDecimal coutHoraire
) {
  PosteDeTravailACreer toDomain() {
    return new PosteDeTravailACreer(libelle, nature, coutHoraire);
  }
}
