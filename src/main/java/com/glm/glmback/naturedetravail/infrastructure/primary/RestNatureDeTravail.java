package com.glm.glmback.naturedetravail.infrastructure.primary;

import com.glm.glmback.naturedetravail.domain.NatureDeTravail;
import com.glm.glmback.naturedetravail.domain.NatureDeTravailListee;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Une nature de travail de l'entreprise.")
record RestNatureDeTravail(
  @Schema(description = "Identifiant de la nature.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(
    description = "Libelle de la nature, affiche partout ou elle apparait.",
    example = "Soudage",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  String libelle,
  @Schema(
    description = "Un poste ou un pointage se sert de la nature : elle ne peut pas etre supprimee.",
    example = "true",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  boolean utilisee
) {
  static RestNatureDeTravail from(NatureDeTravailListee listee) {
    return new RestNatureDeTravail(listee.nature().id().uuid(), listee.nature().libelle().value(), listee.utilisee());
  }

  static RestNatureDeTravail declaree(NatureDeTravail nature) {
    return from(new NatureDeTravailListee(nature, false));
  }
}
