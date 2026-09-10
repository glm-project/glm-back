package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.Pointage;
import com.glm.glmback.syntheseheures.domain.TypeDEvenementDePresence;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(
  name = "RestPointageDeSyntheseDesHeures",
  description = """
  Un pointage du journal de presence, tel qu'il figure dans le releve.

  Un pointage dont l'enchainement casse l'automate de presence (deux arrivees sans depart entre les deux, par
  exemple) reste visible, marque invalide : il n'empeche jamais la generation du releve, mais n'entre pour rien
  dans le calcul de la duree du jour. C'est le signal qu'une correction est attendue sur le journal d'atelier.
  """
)
record RestPointage(
  @Schema(description = "Nature du pointage.", requiredMode = Schema.RequiredMode.REQUIRED) TypeDEvenementDePresence type,
  @Schema(description = "Heure metier a laquelle le pointage a eu lieu.", requiredMode = Schema.RequiredMode.REQUIRED) Instant dateDeSurvenue,
  @Schema(
    description = "Faux si ce pointage ne s'enchaine pas selon l'automate de presence — une correction est attendue.",
    requiredMode = Schema.RequiredMode.REQUIRED
  ) boolean valide
) {
  static RestPointage from(Pointage pointage) {
    return new RestPointage(pointage.evenement().type(), pointage.evenement().dateDeSurvenue(), pointage.valide());
  }
}
