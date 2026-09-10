package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.OperateurConnu;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
  name = "RestOperateurDeSyntheseDesHeures",
  description = """
  L'operateur dont la synthese des heures est lue, resolu au referentiel a la lecture.

  Nom et prenom sont relus a chaque appel : une fiche corrigee s'affiche corrigee sur tout l'historique.
  """
)
record RestOperateur(
  @Schema(description = "Identifiant de l'operateur dans le referentiel.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Nom de l'operateur.", example = "Dupont", requiredMode = Schema.RequiredMode.REQUIRED) String nom,
  @Schema(description = "Prenom de l'operateur.", example = "Jean", requiredMode = Schema.RequiredMode.REQUIRED) String prenom
) {
  static RestOperateur from(OperateurConnu operateur) {
    return new RestOperateur(operateur.id().uuid(), operateur.nom().value(), operateur.prenom().value());
  }
}
