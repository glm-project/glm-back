package com.glm.glmback.naturedetravail.infrastructure.primary;

import com.glm.glmback.naturedetravail.domain.LibelleDeNature;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Libelle d'une nature de travail.")
record RestLibelleDeNature(
  @Schema(
    description = "Libelle de la nature, de 1 a 50 caracteres une fois les espaces qui l'entourent retires. Unique dans l'entreprise, a la casse et aux accents pres.",
    example = "Soudage",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotBlank
  @Pattern(regexp = DE_UN_A_CINQUANTE_CARACTERES_UNE_FOIS_ROGNE)
  String libelle
) {
  /**
   * Le domaine rogne le libelle avant d'en mesurer la longueur : la borne se juge ici de meme, pour qu'un libelle trop
   * long soit refuse en 400 sans atteindre le domaine.
   */
  private static final String DE_UN_A_CINQUANTE_CARACTERES_UNE_FOIS_ROGNE = "\\s*\\S(.{0,48}\\S)?\\s*";

  LibelleDeNature toDomain() {
    return new LibelleDeNature(libelle);
  }
}
