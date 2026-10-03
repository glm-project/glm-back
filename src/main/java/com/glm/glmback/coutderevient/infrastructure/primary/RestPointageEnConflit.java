package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.PointageEnConflit;
import com.glm.glmback.coutderevient.domain.TypeDePointage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  name = "RestPointageEnConflitDuCout",
  description = "Un fait actif contradictoire de la sequence qui laisse le pointage a resoudre."
)
record RestPointageEnConflit(
  @Schema(description = "Identite du fait.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Ce que le fait disait.", requiredMode = Schema.RequiredMode.REQUIRED) TypeDePointage type,
  @Schema(description = "Quand il est survenu.", requiredMode = Schema.RequiredMode.REQUIRED) Instant survenue
) {
  static RestPointageEnConflit from(PointageEnConflit pointage) {
    return new RestPointageEnConflit(pointage.evenement(), pointage.type(), pointage.survenue());
  }
}
