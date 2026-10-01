package com.glm.glmback.atelier.infrastructure.primary;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

record RestSupervisionDAtelier(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant evaluation,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RestOperateurDeSupervision> operateurs,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RestActiviteEnCours> activites,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RestSequenceEnConflit> sequencesEnConflit
) {}
