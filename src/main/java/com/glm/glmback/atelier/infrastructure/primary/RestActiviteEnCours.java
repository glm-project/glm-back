package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.ActiviteEnCours;
import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.PosteDeTravail;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Ce que l'ecran d'atelier affiche : qui fait quoi, dans quel etat, depuis quand.")
record RestActiviteEnCours(
  @Schema(description = "Operateur en activite.", example = "dupont") String operateur,
  @Schema(description = "Poste de travail, facultatif.", example = "fraiseuse-1") String poste,
  @Schema(description = "TRAVAIL ou NON_CONFORMITE.") CategorieDActivite categorie,
  @Schema(description = "Instant depuis lequel cette activite dure.") Instant depuis
) {
  static RestActiviteEnCours from(ActiviteEnCours activite) {
    return new RestActiviteEnCours(
      activite.operateur().value(),
      activite.poste().map(PosteDeTravail::value).orElse(null),
      activite.categorie(),
      activite.depuis()
    );
  }
}
