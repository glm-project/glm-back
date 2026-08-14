package com.glm.glmback.feuilledetemps.infrastructure.primary;

import com.glm.glmback.feuilledetemps.domain.JourDeLaSemaine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(
  description = """
  Un jour du calendrier de l'entreprise, et la presence qui lui revient.

  Les sept jours sont toujours rendus, meme vides : un trou obligerait le lecteur a deviner s'il manque une journee
  ou si l'operateur n'etait pas la.
  """
)
record RestJourDeLaSemaine(
  @Schema(description = "Date du jour, dans le fuseau de l'entreprise.", example = "2026-05-11") LocalDate jour,
  @Schema(description = "Fenetres de presence de ce jour, dans l'ordre des heures.") List<RestPlage> presence
) {
  static RestJourDeLaSemaine from(JourDeLaSemaine jour) {
    return new RestJourDeLaSemaine(jour.jour(), jour.presence().stream().map(RestPlage::from).toList());
  }
}
