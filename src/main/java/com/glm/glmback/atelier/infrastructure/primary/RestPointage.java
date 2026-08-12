package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.Operateur;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PosteDeTravail;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Optional;

@Schema(description = "Un pointage de l'operateur sur un element engage, date a l'instant present.")
record RestPointage(
  @Schema(
    description = "DEBUT, NON_CONFORMITE ou FIN. Une reprise apres non conformite se pointe comme un DEBUT.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  TypeDEvenementDAtelier type,

  @Schema(description = "Operateur dont le temps est affecte.", example = "dupont", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  @Size(max = 100)
  String operateur,

  @Schema(description = "Poste de travail. Toujours facultatif : une entreprise sans parc machine le laisse vide.", example = "fraiseuse-1")
  @Size(max = 100)
  String poste
) {
  PointageAEnregistrer toDomain(SuiviDAtelierId suivi) {
    return PointageAEnregistrer.builder()
      .suivi(suivi)
      .type(type)
      .operateur(new Operateur(operateur))
      .poste(Optional.ofNullable(poste).map(PosteDeTravail::new));
  }
}
