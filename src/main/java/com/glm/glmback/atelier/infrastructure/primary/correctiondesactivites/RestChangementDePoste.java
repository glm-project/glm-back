package com.glm.glmback.atelier.infrastructure.primary.correctiondesactivites;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.correctiondesactivites.ChangementDePosteAEnregistrer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(
  description = """
  Le nouveau poste d'une activite terminee. L'operateur, la categorie et les heures sont ceux de l'activite ; la nature
  d'operation et le cout horaire sont recopies du nouveau poste au moment de la correction, comme au pointage, et le taux
  horaire reste celui de l'activite.

  Le fait CHANGEMENT_DE_POSTE est date du moment de la correction, vise l'activite par `activiteVisee` et ne modifie aucun
  fait : l'activite compte sur le nouveau poste dans toutes les lectures du journal.
  """
)
record RestChangementDePoste(
  @Schema(
    description = """
    Identifiant de la saisie, genere par le client une fois par saisie et conserve d'un renvoi a l'autre. Un identifiant
    deja present au journal est un renvoi : il repond 200, sans rien ecrire, avant toute regle.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  UUID id,

  @Schema(
    description = "Identifiant du nouveau poste de travail, different du poste actuel et sur lequel l'operateur est habilite.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  UUID poste
) {
  ChangementDePosteAEnregistrer toDomain(SuiviDAtelierId suivi, ActiviteId activite, Auteur auteur) {
    return ChangementDePosteAEnregistrer.builder()
      .suivi(suivi)
      .evenement(new EvenementDAtelierId(id))
      .activite(activite)
      .poste(new PosteDeTravailId(poste))
      .auteur(auteur);
  }
}
