package com.glm.glmback.parametrage.infrastructure.primary;

import com.glm.glmback.parametrage.domain.Logo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Le logo de l'entreprise.")
record RestLogo(
  @Schema(
    description = "Empreinte du contenu du logo : elle change avec le logo, et entre dans l'adresse de l'image pour que le navigateur la garde en cache.",
    example = "3f2a9c41b07d58e6",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  String version
) {
  static RestLogo from(Logo logo) {
    return new RestLogo(logo.version().value());
  }
}
