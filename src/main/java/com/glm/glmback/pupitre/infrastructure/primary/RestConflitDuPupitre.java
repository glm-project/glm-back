package com.glm.glmback.pupitre.infrastructure.primary;

import com.glm.glmback.pupitre.domain.ActiviteId;
import com.glm.glmback.pupitre.domain.PointageId;
import com.glm.glmback.pupitre.domain.PosteDeTravailId;
import com.glm.glmback.pupitre.domain.SequenceEnConflitDuPupitre;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(
  name = "RestConflitDuPupitre",
  description = "Sequence interpretee en conflit par atelier. Aucune de ses activites n est actionnable ; une nouvelle ouverture reste possible."
)
record RestConflitDuPupitre(
  @Schema(description = "Operateur de la sequence.", requiredMode = Schema.RequiredMode.REQUIRED) UUID operateur,
  @Schema(description = "Poste de la sequence, absent si aucun poste n a ete pointe.") UUID poste,
  @Schema(
    description = "Identites stables des activites a resoudre, dans l ordre des ouvertures. Peut etre vide.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<UUID> activites,
  @Schema(description = "Identites des pointages de la sequence, dans l ordre du journal.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<UUID> pointages
) {
  static RestConflitDuPupitre from(SequenceEnConflitDuPupitre conflit) {
    return new RestConflitDuPupitre(
      conflit.operateur().uuid(),
      conflit.poste().map(PosteDeTravailId::uuid).orElse(null),
      conflit.activites().stream().map(ActiviteId::uuid).toList(),
      conflit.pointages().stream().map(PointageId::uuid).toList()
    );
  }
}
