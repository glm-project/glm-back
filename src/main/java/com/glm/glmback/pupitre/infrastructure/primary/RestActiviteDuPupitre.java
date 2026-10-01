package com.glm.glmback.pupitre.infrastructure.primary;

import com.glm.glmback.pupitre.domain.ActivitePointable;
import com.glm.glmback.pupitre.domain.CategorieDActivite;
import com.glm.glmback.pupitre.domain.PosteDeTravailId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  name = "RestActiviteDuPupitre",
  description = """
  Une activite pointable a genereLe, sans fin ni conflit : qui travaille, sur quel poste, depuis quand
  et jusqu'a quelle echeance. Son ouverture stable est la cible des fins et transitions.
  """
)
record RestActiviteDuPupitre(
  @Schema(description = "Identifiant de l'operateur en activite.", requiredMode = Schema.RequiredMode.REQUIRED) UUID operateur,
  @Schema(description = "Identifiant du poste de travail, absent si aucun n'a ete pointe.") UUID poste,
  @Schema(description = "TRAVAIL ou NON_CONFORMITE.", requiredMode = Schema.RequiredMode.REQUIRED) CategorieDActivite categorie,
  @Schema(description = "Instant depuis lequel cette activite dure.", requiredMode = Schema.RequiredMode.REQUIRED) Instant depuis,
  @Schema(description = "Identite stable de l'activite, cible des fins et transitions.", requiredMode = Schema.RequiredMode.REQUIRED)
  UUID ouverture,
  @Schema(description = "Echeance publiee par l'atelier.", requiredMode = Schema.RequiredMode.REQUIRED) Instant echeance
) {
  static RestActiviteDuPupitre from(ActivitePointable activite) {
    return new RestActiviteDuPupitre(
      activite.operateur().uuid(),
      activite.poste().map(PosteDeTravailId::uuid).orElse(null),
      activite.categorie(),
      activite.depuis(),
      activite.ouverture().id(),
      activite.echeance()
    );
  }
}
