package com.glm.glmback.atelier.infrastructure.primary.correctiondesactivites;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.correctiondesactivites.RetraitAEnregistrer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(
  description = """
  Le retrait d'une activite terminee. Rien n'est supprime : le fait RETRAIT, date du moment de la correction, vise l'activite
  par `activiteVisee` et l'ecarte de toutes les lectures du journal.
  """
)
record RestRetrait(
  @Schema(
    description = """
    Identifiant de la saisie, genere par le client une fois par saisie et conserve d'un renvoi a l'autre. Un identifiant
    deja present au journal est un renvoi : il repond 200, sans rien ecrire, avant toute regle.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  UUID id
) {
  RetraitAEnregistrer toDomain(SuiviDAtelierId suivi, ActiviteId activite, Auteur auteur) {
    return RetraitAEnregistrer.builder().suivi(suivi).evenement(new EvenementDAtelierId(id)).activite(activite).auteur(auteur);
  }
}
