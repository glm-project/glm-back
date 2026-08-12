package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.Operateur;
import com.glm.glmback.atelier.domain.PointageDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(
  description = """
  Une pause, une reprise ou un depart, saisis en direct sur la journee ouverte de l'operateur.

  Un seul evenement, quel que soit le nombre d'elements sur lesquels l'operateur travaille : c'est ce qui donne un
  bouton de pause unique, sans N clics pour N taches. Aucun identifiant de journee n'est necessaire.
  """
)
record RestPointageDePresence(
  @Schema(description = "Operateur qui pointe.", example = "dupont", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  @Size(max = 100)
  String operateur,

  @Schema(
    description = "PAUSE, REPRISE ou DEPART. ARRIVEE passe par POST /api/atelier/journees.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  TypeDEvenementDePresence type
) {
  PointageDePresenceAEnregistrer toDomain(Auteur auteur) {
    return new PointageDePresenceAEnregistrer(new Operateur(operateur), auteur, type);
  }
}
