package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.OperateurDeSupervision;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

record RestOperateurDeSupervision(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nom,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String prenom,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> metiers
) {
  static RestOperateurDeSupervision from(OperateurDeSupervision supervise) {
    return new RestOperateurDeSupervision(
      supervise.operateur().id().uuid(),
      supervise.operateur().nom().value(),
      supervise.operateur().prenom().value(),
      supervise.metiers().stream().map(NatureDOperation::libelle).toList()
    );
  }
}
