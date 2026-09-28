package com.glm.glmback.feuilledetemps.infrastructure.primary;

import com.glm.glmback.feuilledetemps.domain.JourDeLaSemaine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(
  description = """
  Un jour du calendrier de l'entreprise, la presence et le travail qui lui reviennent.

  Les sept jours sont toujours rendus, meme vides : un trou obligerait le lecteur a deviner s'il manque une journee
  ou si l'operateur n'etait pas la.
  """
)
record RestJourDeLaSemaine(
  @Schema(description = "Date du jour, dans le fuseau de l'entreprise.", example = "2026-05-11") LocalDate jour,
  @Schema(description = "Fenetres de presence de ce jour, dans l'ordre des heures.") List<RestPlage> presence,
  @Schema(
    description = """
    Les periodes de travail de ce jour, dans l'ordre des debuts. Chacune est reduite aux fenetres de presence de la
    journee ou elle a commence : un depart referme ce que l'operateur a oublie d'arreter, et un travail commence hors
    de toute journee n'est pas rendu. Deux elements travailles en meme temps donnent deux activites qui se chevauchent ;
    deux activites commencees au meme instant se departagent par l'identifiant de l'element.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<RestActiviteDeLaFeuilleDeTemps> activites
) {
  static RestJourDeLaSemaine from(JourDeLaSemaine jour) {
    return new RestJourDeLaSemaine(
      jour.jour(),
      jour.presence().stream().map(RestPlage::from).toList(),
      jour.activites().stream().map(RestActiviteDeLaFeuilleDeTemps::from).toList()
    );
  }
}
