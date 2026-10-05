package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

record RestAdresseDossierAnomalie(
  @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID suivi,
  @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID pointage
) {}
