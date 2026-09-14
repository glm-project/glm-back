package com.glm.glmback.pupitre.infrastructure.primary;

import com.glm.glmback.pupitre.domain.ActiviteEnCours;
import com.glm.glmback.pupitre.domain.CategorieDActivite;
import com.glm.glmback.pupitre.domain.PosteDeTravailId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  name = "RestActiviteDuPupitre",
  description = """
  Une activite ouverte sur cet element : qui travaille, sur quel poste, dans quel etat, depuis quand.

  Une non conformite ne ferme pas l'activite — ce temps-la se compte aussi. C'est la categorie qui change, pas
  l'etat de l'element.
  """
)
record RestActiviteDuPupitre(
  @Schema(description = "Identifiant de l'operateur en activite.", requiredMode = Schema.RequiredMode.REQUIRED) UUID operateur,
  @Schema(description = "Identifiant du poste de travail, absent si aucun n'a ete pointe.") UUID poste,
  @Schema(description = "TRAVAIL ou NON_CONFORMITE.", requiredMode = Schema.RequiredMode.REQUIRED) CategorieDActivite categorie,
  @Schema(description = "Instant depuis lequel cette activite dure.", requiredMode = Schema.RequiredMode.REQUIRED) Instant depuis
) {
  static RestActiviteDuPupitre from(ActiviteEnCours activite) {
    return new RestActiviteDuPupitre(
      activite.operateur().uuid(),
      activite.poste().map(PosteDeTravailId::uuid).orElse(null),
      activite.categorie(),
      activite.depuis()
    );
  }
}
