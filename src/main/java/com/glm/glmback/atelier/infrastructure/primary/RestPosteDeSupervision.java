package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.PosteDeSupervision;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

record RestPosteDeSupervision(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String libelle,
  String nature
) {
  static RestPosteDeSupervision from(PosteDeSupervision poste) {
    return new RestPosteDeSupervision(poste.id().uuid(), poste.libelle().value(), poste.nature().map(NatureDOperation::value).orElse(null));
  }
}
