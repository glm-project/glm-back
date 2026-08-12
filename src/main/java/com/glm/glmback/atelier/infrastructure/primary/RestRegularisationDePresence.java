package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.RegularisationDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Schema(
  description = """
  Une presence rattrapee par le gestionnaire, sur une journee deja ouverte, a l'heure ou elle a reellement eu lieu.

  Une seule regularisation de depart suffit a refermer tous les elements restes ouverts ce jour-la : le correctif est
  porte par la presence, jamais recopie element par element.
  """
)
record RestRegularisationDePresence(
  @Schema(description = "Nature du pointage rattrape.", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull TypeDEvenementDePresence type,

  @Schema(description = "Heure metier a laquelle le fait a reellement eu lieu.", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  Instant dateDeSurvenue
) {
  RegularisationDePresenceAEnregistrer toDomain(JourneeDeTravailId journee, Auteur auteur) {
    return RegularisationDePresenceAEnregistrer.builder().journee(journee).type(type).auteur(auteur).dateDeSurvenue(dateDeSurvenue);
  }
}
