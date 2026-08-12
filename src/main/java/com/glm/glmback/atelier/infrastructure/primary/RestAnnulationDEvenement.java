package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.AnnulationAEnregistrer;
import com.glm.glmback.atelier.domain.AnnulationDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.EvenementDePresenceId;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.MotifDAnnulation;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(
  description = """
  L'annulation d'une saisie en trop.

  L'evenement n'est pas supprime : il reste au journal, porteur de son annulation, et le repli l'ecarte.
  """
)
record RestAnnulationDEvenement(
  @Schema(description = "Motif de l'annulation.", example = "Erreur de saisie", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  @Size(max = 255)
  String motif
) {
  AnnulationAEnregistrer toDomain(SuiviDAtelierId suivi, UUID evenement, Auteur auteur) {
    return AnnulationAEnregistrer.builder()
      .suivi(suivi)
      .evenement(new EvenementDAtelierId(evenement))
      .auteur(auteur)
      .motif(new MotifDAnnulation(motif));
  }

  AnnulationDePresenceAEnregistrer toDomain(JourneeDeTravailId journee, UUID evenement, Auteur auteur) {
    return AnnulationDePresenceAEnregistrer.builder()
      .journee(journee)
      .evenement(new EvenementDePresenceId(evenement))
      .auteur(auteur)
      .motif(new MotifDAnnulation(motif));
  }
}
