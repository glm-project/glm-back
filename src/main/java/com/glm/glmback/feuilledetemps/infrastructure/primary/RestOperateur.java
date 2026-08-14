package com.glm.glmback.feuilledetemps.infrastructure.primary;

import com.glm.glmback.feuilledetemps.domain.OperateurConnu;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
  description = """
  L'operateur dont la feuille de temps est lue, resolu au referentiel a la lecture.

  Nom et prenom sont relus a chaque appel : une fiche corrigee s'affiche corrigee sur tout l'historique.
  """
)
record RestOperateur(
  @Schema(description = "Identifiant de l'operateur dans le referentiel.") UUID id,
  @Schema(description = "Nom de l'operateur.", example = "Dupont") String nom,
  @Schema(description = "Prenom de l'operateur.", example = "Jean") String prenom
) {
  static RestOperateur from(OperateurConnu operateur) {
    return new RestOperateur(operateur.id().uuid(), operateur.nom().value(), operateur.prenom().value());
  }
}
