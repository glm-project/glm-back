package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.SequenceEnConflitDeSupervision;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

record RestSequenceEnConflitDeSupervision(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID operateurId,
  RestPosteDeSupervision poste,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RestDescriptionDActiviteDeSupervision> activites
) {
  static RestSequenceEnConflitDeSupervision from(SequenceEnConflitDeSupervision sequence) {
    return new RestSequenceEnConflitDeSupervision(
      sequence.id().uuid(),
      sequence.operateur().uuid(),
      sequence.poste().map(RestPosteDeSupervision::from).orElse(null),
      sequence.activites().stream().map(RestDescriptionDActiviteDeSupervision::from).toList()
    );
  }
}
