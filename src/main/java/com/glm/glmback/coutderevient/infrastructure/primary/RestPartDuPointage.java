package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.ActiviteInterpretee;
import com.glm.glmback.coutderevient.domain.AnnuaireDuCout;
import com.glm.glmback.coutderevient.domain.Diviseur;
import com.glm.glmback.coutderevient.domain.MontantTotal;
import com.glm.glmback.coutderevient.domain.TrancheValorisable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Schema(
  name = "RestPartDuPointageDuCout",
  description = """
  Une part d'un pointage termine : la periode ou l'operateur occupait le meme ensemble de postes.

  Sa main d'oeuvre vaut taux horaire x duree / diviseur, deja repartie au centime dans la fenetre de partage. Elle est
  incomplete, sans diviseur, quand un pointage a resoudre de l'operateur, sur un autre poste, empeche de la partager.
  """
)
record RestPartDuPointage(
  @Schema(description = "Debut de la part.", requiredMode = Schema.RequiredMode.REQUIRED) Instant debut,
  @Schema(description = "Fin de la part.", requiredMode = Schema.RequiredMode.REQUIRED) Instant fin,
  @Schema(description = "Duree ISO-8601 de la part.", example = "PT1H30M", requiredMode = Schema.RequiredMode.REQUIRED) Duration duree,
  @Schema(description = "Nombre de postes occupes en meme temps, absent quand le partage est inconnu.", example = "2") Integer diviseur,
  @Schema(description = "Main d'oeuvre de la part, au centime.", requiredMode = Schema.RequiredMode.REQUIRED) RestMontantDuCout mainDOeuvre,
  @Schema(description = "Ce que l'operateur menait d'autre pendant la part.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestActiviteCitee> paralleles,
  @Schema(description = "Les activites a resoudre qui empechent de connaitre le diviseur.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestActiviteCitee> bloquants
) {
  static RestPartDuPointage from(TrancheValorisable part, AnnuaireDuCout annuaire) {
    return new RestPartDuPointage(
      part.tranche().periode().debut(),
      part.tranche().periode().fin(),
      part.tranche().duree(),
      part.diviseur().map(Diviseur::value).orElse(null),
      RestMontantDuCout.from(part.coutDeMainDOeuvre().map(MontantTotal::de).orElseGet(MontantTotal::incomplet)),
      part
        .paralleles()
        .stream()
        .map(activite -> RestActiviteCitee.from(activite, annuaire))
        .toList(),
      part
        .responsables()
        .stream()
        .sorted(
          Comparator.comparing((ActiviteInterpretee responsable) -> responsable.plage().debut()).thenComparing(responsable ->
            responsable.id().uuid()
          )
        )
        .map(responsable -> RestActiviteCitee.from(responsable.activite(), annuaire))
        .toList()
    );
  }
}
