package com.glm.glmback.postedetravail.infrastructure.primary;

import com.glm.glmback.postedetravail.domain.NatureDeTravailId;
import com.glm.glmback.postedetravail.domain.PosteDeTravailAModifier;
import com.glm.glmback.postedetravail.domain.PosteDeTravailId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Schema(description = "Revision d'un poste de travail.")
record RestModificationPosteDeTravail(
  @Schema(description = "Nom du poste, unique dans l'entreprise.", example = "Tour 1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  @Size(max = 100)
  String libelle,

  @Schema(
    description = "Identifiant de la nature du poste, choisie dans le referentiel des natures de travail.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  UUID natureId,

  @Schema(
    description = "Cout horaire du poste, laisse vide pour le retirer. Positif ou nul (0 pour un poste qui ne demande que de la main d'oeuvre), exactement representable en centimes et inferieur a 100000000.",
    example = "45.50"
  )
  @DecimalMin("0")
  @Digits(integer = 8, fraction = 2)
  BigDecimal coutHoraire
) {
  RestModificationPosteDeTravail {
    coutHoraire = Optional.ofNullable(coutHoraire).map(BigDecimal::stripTrailingZeros).orElse(null);
  }

  PosteDeTravailAModifier toDomain(PosteDeTravailId id) {
    return new PosteDeTravailAModifier(id, libelle, new NatureDeTravailId(natureId), coutHoraire);
  }
}
