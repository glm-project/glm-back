package com.glm.glmback.feuilledetemps.infrastructure.primary;

import com.glm.glmback.feuilledetemps.domain.JourDeLaSemaine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Un des sept jours de la semaine ISO, avec ses portions d'activite, meme vides.")
record RestJourDeLaSemaine(
  @Schema(description = "Date du jour, dans le fuseau de l'entreprise.", example = "2026-05-11") LocalDate jour,
  @Schema(
    description = """
    Les portions d'activite de ce jour, triees par debut, element puis identite stable de l'activite.
    Deux elements travailles en meme temps donnent deux portions qui se chevauchent.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<RestActiviteDeLaFeuilleDeTemps> activites
) {
  static RestJourDeLaSemaine from(JourDeLaSemaine jour) {
    return new RestJourDeLaSemaine(jour.jour(), jour.activites().stream().map(RestActiviteDeLaFeuilleDeTemps::from).toList());
  }
}
