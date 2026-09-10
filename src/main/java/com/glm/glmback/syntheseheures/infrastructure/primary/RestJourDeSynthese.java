package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.JourDeSynthese;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Schema(
  description = """
  Un jour du calendrier de l'entreprise, ses pointages et la duree travaillee qui lui revient.

  Les sept jours sont toujours rendus, meme vides : un trou obligerait le lecteur a deviner s'il manque une journee
  ou si l'operateur n'etait pas la.
  """
)
record RestJourDeSynthese(
  @Schema(description = "Date du jour, dans le fuseau de l'entreprise.", example = "2026-05-11") LocalDate jour,
  @Schema(description = "Les pointages de ce jour, dans l'ordre des heures.") List<RestPointage> pointages,
  @Schema(description = "Duree travaillee du jour, pauses exclues. Ignore tout pointage invalide.", example = "PT8H") Duration duree,
  @Schema(description = "Vrai des qu'un pointage de ce jour casse l'automate de presence.") boolean aUneAnomalie
) {
  static RestJourDeSynthese from(JourDeSynthese jour) {
    return new RestJourDeSynthese(
      jour.jour(),
      jour.pointages().stream().map(RestPointage::from).toList(),
      jour.duree(),
      jour.aUneAnomalie()
    );
  }
}
