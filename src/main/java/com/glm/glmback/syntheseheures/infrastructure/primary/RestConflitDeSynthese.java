package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.ActiviteId;
import com.glm.glmback.syntheseheures.domain.PointageId;
import com.glm.glmback.syntheseheures.domain.PosteDeTravailId;
import com.glm.glmback.syntheseheures.domain.SequenceEnConflit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Sequence en conflit de l'operateur, rendue lorsqu'une activite ou un pointage concerne la semaine.")
record RestConflitDeSynthese(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID element,
  @Schema(description = "Poste lorsqu'il est connu, absent sinon.") UUID poste,
  @Schema(
    description = "Identites stables des activites, dans l'ordre de la sequence ; peut etre vide.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<UUID> activites,
  @Schema(
    description = "Identites des pointages en ordre metier, meme sans activite interpretable.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<UUID> pointages
) {
  static RestConflitDeSynthese from(SequenceEnConflit conflit) {
    return new RestConflitDeSynthese(
      conflit.element().uuid(),
      conflit.poste().map(PosteDeTravailId::uuid).orElse(null),
      conflit.activites().stream().map(ActiviteId::uuid).toList(),
      conflit.pointages().stream().map(PointageId::uuid).toList()
    );
  }
}
