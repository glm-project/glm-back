package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.CorrectionDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.EvenementDePresenceId;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.MotifDAnnulation;
import com.glm.glmback.atelier.domain.RegularisationDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "La correction d'une presence fausse : une annulation et une regularisation en un seul acte.")
record RestCorrectionDePresence(
  @Schema(description = "Motif de l'annulation.", example = "Heure erronee", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  @Size(max = 255)
  String motif,

  @Schema(description = "Nature du pointage corrige.", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull TypeDEvenementDePresence type,

  @Schema(description = "Heure metier corrigee.", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Instant dateDeSurvenue
) {
  CorrectionDePresenceAEnregistrer toDomain(JourneeDeTravailId journee, UUID evenement, Auteur auteur) {
    RegularisationDePresenceAEnregistrer remplacement = RegularisationDePresenceAEnregistrer.builder()
      .journee(journee)
      .type(type)
      .auteur(auteur)
      .dateDeSurvenue(dateDeSurvenue);

    return new CorrectionDePresenceAEnregistrer(new EvenementDePresenceId(evenement), new MotifDAnnulation(motif), remplacement);
  }
}
