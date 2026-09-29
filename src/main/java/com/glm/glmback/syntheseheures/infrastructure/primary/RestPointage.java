package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.PointageDElement;
import com.glm.glmback.syntheseheures.domain.PosteDeTravailId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  name = "RestPointageDeSyntheseDesHeures",
  description = """
  Un pointage du journal brut du jour, tel qu'il figure dans le releve : un geste sur un element.
  """
)
record RestPointage(
  @Schema(description = "Nature du pointage.", requiredMode = Schema.RequiredMode.REQUIRED) RestTypeDePointage type,
  @Schema(description = "Heure metier a laquelle le pointage a eu lieu.", requiredMode = Schema.RequiredMode.REQUIRED)
  Instant dateDeSurvenue,
  @Schema(description = "L'element pointe.", requiredMode = Schema.RequiredMode.REQUIRED) UUID element,
  @Schema(description = "Le poste pointe, present pour un pointage d'element quand il a ete pointe.") UUID poste
) {
  static RestPointage from(PointageDElement element) {
    return new RestPointage(
      RestTypeDePointage.from(element.type()),
      element.dateDeSurvenue(),
      element.element().uuid(),
      element.poste().map(PosteDeTravailId::uuid).orElse(null)
    );
  }
}
