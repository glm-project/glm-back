package com.glm.glmback.atelier.infrastructure.primary.correctiondesactivites;

import com.glm.glmback.atelier.domain.correctiondesactivites.JourneeACorriger;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
  description = """
  Ce que le gestionnaire peut choisir pour ajouter une activite ce jour-la : les suivis presents (un par suivi) et les
  postes sur lesquels l'operateur est habilite aujourd'hui.
  """
)
record RestChoixDAjout(
  @Schema(description = "Les suivis presents a l'atelier ce jour-la, par engagement.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestSuiviPresent> suivis,
  @Schema(description = "Les postes habilites de l'operateur, par libelle.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestPosteDeLaJournee> postes
) {
  static RestChoixDAjout from(JourneeACorriger journee) {
    return new RestChoixDAjout(
      journee
        .choix()
        .suivis()
        .stream()
        .map(suivi -> RestSuiviPresent.from(suivi, journee))
        .toList(),
      journee.choix().postes().stream().map(RestPosteDeLaJournee::from).toList()
    );
  }
}
