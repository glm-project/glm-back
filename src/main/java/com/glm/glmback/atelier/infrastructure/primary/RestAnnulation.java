package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.Annulation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "L'annulation d'un evenement. L'evenement reste au journal, porteur de cette annulation.")
record RestAnnulation(
  @Schema(description = "Utilisateur ayant annule l'evenement.", example = "gestionnaire.impeccmold") String auteur,
  @Schema(description = "Instant auquel l'annulation a ete saisie.") Instant date,
  @Schema(description = "Motif saisi par le gestionnaire.", example = "Erreur de saisie") String motif
) {
  static RestAnnulation from(Annulation annulation) {
    return new RestAnnulation(annulation.auteur().value(), annulation.date(), annulation.motif().value());
  }
}
