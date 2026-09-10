package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.EvenementDePresence;
import com.glm.glmback.syntheseheures.domain.TypeDEvenementDePresence;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(
  name = "RestPointageDeSyntheseDesHeures",
  description = "Un pointage du journal de presence, tel qu'il figure dans le releve."
)
record RestPointage(
  @Schema(description = "Nature du pointage.", requiredMode = Schema.RequiredMode.REQUIRED) TypeDEvenementDePresence type,
  @Schema(description = "Heure metier a laquelle le pointage a eu lieu.", requiredMode = Schema.RequiredMode.REQUIRED) Instant dateDeSurvenue
) {
  static RestPointage from(EvenementDePresence evenement) {
    return new RestPointage(evenement.type(), evenement.dateDeSurvenue());
  }
}
