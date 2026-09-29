package com.glm.glmback.feuilledetemps.infrastructure.primary;

import com.glm.glmback.feuilledetemps.domain.CategorieDActivite;
import com.glm.glmback.feuilledetemps.domain.IntervalleDActivite;
import com.glm.glmback.feuilledetemps.domain.NatureDOperation;
import com.glm.glmback.feuilledetemps.domain.PosteDeTravailId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  name = "RestActiviteDeLaFeuilleDeTemps",
  description = """
  Une periode de travail de l'operateur sur un element, ramenee au jour qui la porte.

  Le champ activite porte l'identite, les bornes entieres et l'etat explicite. Une activite en cours ou a resoudre
  n'a pas de fin ; une fin automatique porte l'echeance et signale son anomalie.
  """
)
record RestActiviteDeLaFeuilleDeTemps(
  @Schema(
    description = "L'element de fabrication travaille, jamais le suivi : un element reengage apres cloture reste le meme element.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  UUID element,
  @Schema(description = "Le poste pointe, s'il l'a ete. Son libelle est dans la synthese des heures.") UUID poste,
  @Schema(description = "La nature de l'operation, figee a la saisie depuis le poste.", example = "Fraisage") String nature,
  @Schema(description = "Du bon travail, ou sa reprise sur non conformite.", requiredMode = Schema.RequiredMode.REQUIRED)
  CategorieDActivite categorie,
  @Schema(description = "Debut de l'activite.", example = "2026-09-21T05:05:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
  Instant debut,
  @Schema(description = "Fin de l'activite, absente tant qu'elle est en cours.", example = "2026-09-21T10:00:00Z") Instant fin,
  @Schema(description = "L'activite entiere et son etat a la lecture.", requiredMode = Schema.RequiredMode.REQUIRED)
  RestActiviteInterpreteeDeLaFeuilleDeTemps activite
) {
  static RestActiviteDeLaFeuilleDeTemps from(IntervalleDActivite intervalle) {
    return new RestActiviteDeLaFeuilleDeTemps(
      intervalle.activite().element().uuid(),
      intervalle.activite().poste().map(PosteDeTravailId::uuid).orElse(null),
      intervalle.activite().nature().map(NatureDOperation::value).orElse(null),
      intervalle.activite().categorie(),
      intervalle.plage().debut(),
      intervalle.plage().fin().orElse(null),
      RestActiviteInterpreteeDeLaFeuilleDeTemps.from(intervalle.lecture())
    );
  }
}
