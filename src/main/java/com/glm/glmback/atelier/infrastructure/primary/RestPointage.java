package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Schema(description = "Un pointage de l'operateur sur un element engage, date a l'instant present.")
record RestPointage(
  @Schema(description = "Identifiant durable du geste, genere par le pupitre.", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  UUID id,

  @Schema(
    description = "DEBUT, NON_CONFORMITE ou FIN. Une reprise apres non conformite se pointe comme un DEBUT.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  TypeDEvenementDAtelier type,

  @Schema(description = "Identifiant de l'operateur dont le temps est affecte.", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  UUID operateur,

  @Schema(description = "Identifiant du poste de travail. Toujours facultatif : une entreprise sans parc machine le laisse vide.")
  UUID poste,

  @Schema(description = "Heure metier du geste. Absente, elle vaut l'instant de reception initial.") Instant dateDeSurvenue
) {
  PointageAEnregistrer toDomain(SuiviDAtelierId suivi, Auteur auteur) {
    return PointageAEnregistrer.of(
      suivi,
      type,
      new OperateurId(operateur),
      Optional.ofNullable(poste).map(PosteDeTravailId::new),
      auteur,
      Optional.ofNullable(dateDeSurvenue),
      new EvenementDAtelierId(id)
    );
  }
}
