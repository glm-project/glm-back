package com.glm.glmback.postedetravail.infrastructure.primary;

import com.glm.glmback.postedetravail.domain.CoutHoraire;
import com.glm.glmback.postedetravail.domain.PosteDeTravail;
import com.glm.glmback.shared.authentication.application.HourlyRatesAuthorization;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Un poste de travail du referentiel de l'entreprise.")
record RestPosteDeTravail(
  @Schema(description = "Identifiant du poste.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,

  @Schema(description = "Nom du poste tel que l'atelier le designe.", example = "Tour 1", requiredMode = Schema.RequiredMode.REQUIRED)
  String libelle,

  @Schema(
    description = "Identifiant de la nature du poste, dans le referentiel des natures de travail.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  UUID natureId,

  @Schema(
    description = "Libelle courant de la nature du poste : celui du referentiel, relu a chaque lecture.",
    example = "tournage",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  String nature,

  @Schema(
    description = "Cout horaire du poste, absent si l'entreprise ne le valorise pas. Reserve au GESTIONNAIRE, absent pour les autres roles.",
    example = "45.50"
  )
  BigDecimal coutHoraire
) {
  static RestPosteDeTravail from(PosteDeTravail poste) {
    return new RestPosteDeTravail(
      poste.id().uuid(),
      poste.libelle().value(),
      poste.nature().id().uuid(),
      poste.nature().libelle().value(),
      HourlyRatesAuthorization.disclose(poste.coutHoraire().map(CoutHoraire::value))
    );
  }
}
