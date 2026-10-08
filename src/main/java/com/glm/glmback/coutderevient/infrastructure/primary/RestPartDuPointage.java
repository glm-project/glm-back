package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.AnnuaireDuCout;
import com.glm.glmback.coutderevient.domain.MontantTotal;
import com.glm.glmback.coutderevient.domain.TrancheValorisable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Schema(
  name = "RestPartDuPointageDuCout",
  description = """
  Une part d'un pointage termine : la periode ou l'operateur occupait le meme ensemble de postes.

  Sa main d'oeuvre vaut taux horaire x duree / diviseur, deja repartie au centime dans la fenetre de partage.
  """
)
record RestPartDuPointage(
  @Schema(description = "Debut de la part.", requiredMode = Schema.RequiredMode.REQUIRED) Instant debut,
  @Schema(description = "Fin de la part.", requiredMode = Schema.RequiredMode.REQUIRED) Instant fin,
  @Schema(description = "Duree ISO-8601 de la part.", example = "PT1H30M", requiredMode = Schema.RequiredMode.REQUIRED) Duration duree,
  @Schema(description = "Nombre de postes occupes en meme temps.", example = "2", requiredMode = Schema.RequiredMode.REQUIRED) int diviseur,
  @Schema(description = "Main d'oeuvre de la part, au centime.", requiredMode = Schema.RequiredMode.REQUIRED) RestMontantDuCout mainDOeuvre,
  @Schema(description = "Ce que l'operateur menait d'autre pendant la part.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestActiviteCitee> paralleles
) {
  static RestPartDuPointage from(TrancheValorisable part, AnnuaireDuCout annuaire) {
    return new RestPartDuPointage(
      part.tranche().periode().debut(),
      part.tranche().periode().fin(),
      part.tranche().duree(),
      part.diviseur().value(),
      RestMontantDuCout.from(MontantTotal.de(part.coutDeMainDOeuvre())),
      part
        .paralleles()
        .stream()
        .map(activite -> RestActiviteCitee.from(activite, annuaire))
        .toList()
    );
  }
}
