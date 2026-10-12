package com.glm.glmback.atelier.infrastructure.primary.correctiondesactivites;

import com.glm.glmback.atelier.domain.correctiondesactivites.BornesDuJour;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(
  description = """
  Les minuits du jour, calcules dans le fuseau horaire de l'entreprise (aujourd'hui Europe/Paris) : le client ne calcule
  aucun minuit. Comme partout dans l'API, les instants se lisent en UTC (suffixe Z) : en Europe/Paris l'ete, minuit du 5
  octobre est `2026-10-04T22:00:00Z`.
  """
)
record RestBornesDuJour(
  @Schema(description = "Debut du jour, inclus : minuit.", example = "2026-10-04T22:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
  Instant debut,
  @Schema(
    description = "Fin du jour, exclue : minuit du lendemain.",
    example = "2026-10-05T22:00:00Z",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Instant fin,
  @Schema(
    description = "Fin du lendemain, exclue : la fin d'une activite ajoutee ne la depasse pas.",
    example = "2026-10-06T22:00:00Z",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Instant finDuLendemain
) {
  static RestBornesDuJour from(BornesDuJour bornes) {
    return new RestBornesDuJour(bornes.debut(), bornes.fin(), bornes.finDuLendemain());
  }
}
