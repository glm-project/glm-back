package com.glm.glmback.pupitre.infrastructure.primary;

import com.glm.glmback.pupitre.domain.ActiviteSansFin;
import com.glm.glmback.pupitre.domain.CategorieDActivite;
import com.glm.glmback.pupitre.domain.PosteDeTravailId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  name = "RestActiviteDuPupitre",
  description = """
  Une activite ouverte sur cet element : qui travaille, sur quel poste, dans quel etat, depuis quand.

  Une transition ouvre une activite distincte de l autre categorie. Les activites dont l echeance est atteinte
  ne sont pas actionnables et ne figurent pas ici.
  """
)
record RestActiviteDuPupitre(
  @Schema(description = "Identifiant de l'operateur en activite.", requiredMode = Schema.RequiredMode.REQUIRED) UUID operateur,
  @Schema(description = "Identifiant du poste de travail, absent si aucun n'a ete pointe.") UUID poste,
  @Schema(description = "TRAVAIL ou NON_CONFORMITE.", requiredMode = Schema.RequiredMode.REQUIRED) CategorieDActivite categorie,
  @Schema(description = "Instant depuis lequel cette activite dure.", requiredMode = Schema.RequiredMode.REQUIRED) Instant depuis,
  @Schema(
    description = "Identite stable de l activite : l identifiant de son pointage ouvrant.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  UUID ouverture,
  @Schema(
    description = "Echeance de l activite : elle cesse d etre en cours a cet instant inclus.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Instant echeance
) {
  static RestActiviteDuPupitre from(ActiviteSansFin activite) {
    return new RestActiviteDuPupitre(
      activite.operateur().uuid(),
      activite.poste().map(PosteDeTravailId::uuid).orElse(null),
      activite.categorie(),
      activite.depuis(),
      activite.ouverture().uuid(),
      activite.echeance()
    );
  }
}
