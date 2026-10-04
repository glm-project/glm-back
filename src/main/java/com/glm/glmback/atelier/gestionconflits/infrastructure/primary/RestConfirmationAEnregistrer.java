package com.glm.glmback.atelier.gestionconflits.infrastructure.primary;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

record RestConfirmationAEnregistrer(
  @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID commande,
  @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reference
) {}
