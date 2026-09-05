package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.atelier.domain.OperateurId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
  name = "RestOperateurDAtelier",
  description = """
  Un operateur du referentiel, resolu a la lecture.

  Le journal ne stocke que l'identifiant : nom et prenom sont relus a chaque lecture, donc une fiche corrigee
  s'affiche corrigee sur tout l'historique.
  """
)
record RestOperateur(
  @Schema(description = "Identifiant de l'operateur dans le referentiel.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Nom de l'operateur.", example = "Dupont", requiredMode = Schema.RequiredMode.REQUIRED) String nom,
  @Schema(description = "Prenom de l'operateur.", example = "Jean", requiredMode = Schema.RequiredMode.REQUIRED) String prenom
) {
  static RestOperateur resolu(AnnuaireDAtelier annuaire, OperateurId id) {
    return annuaire.operateur(id).map(RestOperateur::from).orElse(null);
  }

  private static RestOperateur from(OperateurConnu operateur) {
    return new RestOperateur(operateur.id().uuid(), operateur.nom().value(), operateur.prenom().value());
  }
}
