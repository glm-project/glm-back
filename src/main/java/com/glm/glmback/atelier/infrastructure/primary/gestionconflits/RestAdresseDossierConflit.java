package com.glm.glmback.atelier.infrastructure.primary.gestionconflits;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

record RestAdresseDossierConflit(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID suivi,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID pointage
) {}
