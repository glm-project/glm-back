package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.EvenementDePresence;
import com.glm.glmback.syntheseheures.domain.PointageDElement;
import com.glm.glmback.syntheseheures.domain.PointageDuJour;
import com.glm.glmback.syntheseheures.domain.PosteDeTravailId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  name = "RestPointageDeSyntheseDesHeures",
  description = """
  Un pointage du journal brut du jour, tel qu'il figure dans le releve : une arrivee ou un depart, ou un geste sur un
  element.
  """
)
record RestPointage(
  @Schema(description = "Nature du pointage.", requiredMode = Schema.RequiredMode.REQUIRED) RestTypeDePointage type,
  @Schema(description = "Heure metier a laquelle le pointage a eu lieu.", requiredMode = Schema.RequiredMode.REQUIRED)
  Instant dateDeSurvenue,
  @Schema(description = "L'element pointe, present pour DEBUT, NON_CONFORMITE et FIN.") UUID element,
  @Schema(description = "Le poste pointe, present pour un pointage d'element quand il a ete pointe.") UUID poste
) {
  static RestPointage from(PointageDuJour pointage) {
    return switch (pointage) {
      case EvenementDePresence evenement -> new RestPointage(
        RestTypeDePointage.from(evenement.type()),
        evenement.dateDeSurvenue(),
        null,
        null
      );
      case PointageDElement pointageDElement -> new RestPointage(
        RestTypeDePointage.from(pointageDElement.type()),
        pointageDElement.dateDeSurvenue(),
        pointageDElement.element().uuid(),
        pointageDElement.poste().map(PosteDeTravailId::uuid).orElse(null)
      );
    };
  }
}
