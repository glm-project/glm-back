package com.glm.glmback.postedetravail.infrastructure.primary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.glm.glmback.postedetravail.domain.PosteDeTravailACreer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Schema(description = "Declaration d'un poste de travail.")
record RestCreationPosteDeTravail(
  @Schema(description = "Nom du poste, unique dans l'entreprise.", example = "Tour 1", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank
  @Size(max = 100)
  String libelle,

  @Schema(
    description = "Identifiant de la nature du poste, choisie dans le referentiel des natures de travail. Obligatoire, sauf pendant la transition ou le libelle `nature` est encore accepte ; l'emporte sur lui quand les deux sont donnes."
  )
  UUID natureId,

  @Schema(
    description = "Deprecie, remplace par natureId et retire par glm-back#130. Libelle de la nature du poste, designee a la casse, aux accents et aux espaces pres ; declaree dans le referentiel si elle manque.",
    example = "tournage",
    deprecated = true
  )
  @Size(max = 50)
  String nature,

  @Schema(
    description = "Cout horaire du poste, destine au cout de revient. Facultatif : toutes les entreprises ne le valorisent pas. Positif ou nul (0 pour un poste qui ne demande que de la main d'oeuvre), exactement representable en centimes et inferieur a 100000000.",
    example = "45.50"
  )
  @DecimalMin("0")
  @Digits(integer = 8, fraction = 2)
  BigDecimal coutHoraire
) {
  RestCreationPosteDeTravail {
    coutHoraire = Optional.ofNullable(coutHoraire).map(BigDecimal::stripTrailingZeros).orElse(null);
  }

  @AssertTrue(message = "natureId ou nature est obligatoire")
  @JsonIgnore
  @Schema(hidden = true)
  boolean isNatureDesignee() {
    return NatureDemandee.estDesignee(natureId, nature);
  }

  PosteDeTravailACreer toDomain() {
    return new PosteDeTravailACreer(libelle, NatureDemandee.choisie(natureId, nature), coutHoraire);
  }
}
