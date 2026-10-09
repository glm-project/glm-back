package com.glm.glmback.pupitre.infrastructure.primary;

import com.glm.glmback.pupitre.domain.VersionDuLogo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Le logo de l'entreprise, par sa seule version.")
record RestLogoDuPupitre(
  @Schema(
    description = "Empreinte du contenu du logo : elle change avec le logo. L'image se lit a /api/parametrage/logo/{version}.",
    example = "3f2a9c41b07d58e6",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  String version
) {
  static RestLogoDuPupitre from(VersionDuLogo version) {
    return new RestLogoDuPupitre(version.value());
  }
}
