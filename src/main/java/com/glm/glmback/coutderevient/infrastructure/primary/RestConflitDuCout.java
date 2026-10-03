package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Sequence en conflit de l'element ou responsable d'un partage humain incomplet sur un autre element.")
record RestConflitDuCout(
  @Schema(description = "Element de fabrication portant la sequence.", requiredMode = Schema.RequiredMode.REQUIRED) UUID element,
  @Schema(description = "Operateur de la sequence.", requiredMode = Schema.RequiredMode.REQUIRED) UUID operateur,
  @Schema(description = "Poste de la sequence, absent sans poste.") UUID poste,
  @Schema(description = "Identites originales des activites de la sequence, dans leur ordre.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<UUID> activites,
  @Schema(description = "Identites des faits actifs contradictoires, dans leur ordre.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<UUID> pointages
) {
  static RestConflitDuCout from(SequenceEnConflit sequence) {
    return new RestConflitDuCout(
      sequence.element().uuid(),
      sequence.operateur().uuid(),
      sequence.poste().map(PosteDeTravailId::uuid).orElse(null),
      sequence.activites().stream().map(ActiviteId::uuid).toList(),
      sequence.pointages().stream().map(PointageEnConflit::evenement).toList()
    );
  }
}
