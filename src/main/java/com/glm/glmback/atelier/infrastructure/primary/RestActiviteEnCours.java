package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.ActiviteEnCours;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.CategorieDActivite;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Ce que l'ecran d'atelier affiche : qui fait quoi, dans quel etat, depuis quand.")
record RestActiviteEnCours(
  @Schema(description = "Operateur en activite, absent si la fiche n'est plus resolue au referentiel.") RestOperateur operateur,
  @Schema(description = "Poste de travail, facultatif.") RestPosteDeTravail poste,
  @Schema(description = "TRAVAIL ou NON_CONFORMITE.", requiredMode = Schema.RequiredMode.REQUIRED) CategorieDActivite categorie,
  @Schema(description = "Instant depuis lequel cette activite dure.", requiredMode = Schema.RequiredMode.REQUIRED) Instant depuis
) {
  static RestActiviteEnCours from(ActiviteEnCours activite, AnnuaireDAtelier annuaire) {
    return new RestActiviteEnCours(
      RestOperateur.resolu(annuaire, activite.operateur()),
      RestPosteDeTravail.resolu(annuaire, activite.poste()),
      activite.categorie(),
      activite.depuis()
    );
  }
}
