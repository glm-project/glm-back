package com.glm.glmback.postedetravail.infrastructure.primary;

import com.glm.glmback.postedetravail.domain.PosteDeTravailAModifier;
import com.glm.glmback.postedetravail.domain.PosteDeTravailId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Optional;

@Schema(description = "Revision d'un poste de travail.")
record RestModificationPosteDeTravail(
  @Schema(description = "Nom du poste, unique dans l'entreprise.", example = "Tour 1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  @Size(max = 100)
  String libelle,

  @Schema(
    description = "Libelle de la nature du poste, designee a la casse, aux accents et aux espaces pres ; declaree dans le referentiel si elle manque. Metier qui s'exerce sur ce poste.",
    example = "tournage",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotBlank
  @Size(max = 50)
  String nature,

  @Schema(
    description = "Cout horaire du poste, laisse vide pour le retirer. Strictement positif, exactement representable en centimes et inferieur a 100000000.",
    example = "45.50"
  )
  @DecimalMin(value = "0", inclusive = false)
  @Digits(integer = 8, fraction = 2)
  BigDecimal coutHoraire
) {
  RestModificationPosteDeTravail {
    coutHoraire = Optional.ofNullable(coutHoraire).map(BigDecimal::stripTrailingZeros).orElse(null);
  }

  PosteDeTravailAModifier toDomain(PosteDeTravailId id) {
    return new PosteDeTravailAModifier(id, libelle, nature, coutHoraire);
  }
}
