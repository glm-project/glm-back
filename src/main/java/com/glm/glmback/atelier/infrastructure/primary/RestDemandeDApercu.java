package com.glm.glmback.atelier.infrastructure.primary;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

record RestDemandeDApercu(
  @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID commande,
  @NotNull @PositiveOrZero @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long revision,
  @NotNull @Valid @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestActeDeResolution acte
) {}
