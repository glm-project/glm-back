package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.EvenementDePresence;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  description = """
  Un evenement du journal de presence.

  L'horodatage est **bitemporel** : `dateDeSurvenue` est l'heure metier, `dateDEnregistrement` l'heure de la saisie.
  Une regularisation se reconnait a l'ecart entre les deux (`estUneRegularisation`), jamais a l'identite de l'auteur.
  """
)
record RestEvenementDePresence(
  @Schema(description = "Identifiant de l'evenement, a reprendre pour l'annuler ou le corriger.") UUID id,
  @Schema(description = "Nature du pointage.") TypeDEvenementDePresence type,
  @Schema(description = "Utilisateur ayant saisi l'evenement.", example = "user.impeccmold") String auteur,
  @Schema(description = "Heure metier a laquelle le fait a eu lieu.") Instant dateDeSurvenue,
  @Schema(description = "Heure a laquelle la saisie a ete enregistree.") Instant dateDEnregistrement,
  @Schema(description = "Vrai lorsque la saisie a ete faite apres coup.") boolean estUneRegularisation,
  @Schema(description = "Presente lorsque l'evenement a ete annule. L'evenement reste au journal.") RestAnnulation annulation
) {
  static RestEvenementDePresence from(EvenementDePresence evenement) {
    return new RestEvenementDePresence(
      evenement.id().uuid(),
      evenement.type(),
      evenement.auteur().value(),
      evenement.dateDeSurvenue(),
      evenement.dateDEnregistrement(),
      evenement.horodatage().estDifferee(),
      evenement.annulation().map(RestAnnulation::from).orElse(null)
    );
  }
}
