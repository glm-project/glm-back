package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.ActiviteEnCours;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.CategorieDActivite;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  description = """
  Ce que l'ecran d'atelier affiche : qui fait quoi, dans quel etat, depuis quand, et jusqu'a quand au plus tard.

  Seules les activites en cours a l'instant de la lecture y figurent : une activite dont l'echeance est atteinte en
  sort, terminee automatiquement.
  """
)
record RestActiviteEnCours(
  @Schema(description = "Operateur en activite, absent si la fiche n'est plus resolue au referentiel.") RestOperateur operateur,
  @Schema(description = "Poste de travail, facultatif.") RestPosteDeTravail poste,
  @Schema(description = "TRAVAIL ou NON_CONFORMITE.", requiredMode = Schema.RequiredMode.REQUIRED) CategorieDActivite categorie,
  @Schema(description = "Instant depuis lequel cette activite dure.", requiredMode = Schema.RequiredMode.REQUIRED) Instant depuis,
  @Schema(
    description = """
    Identite de l'activite : l'identifiant de son pointage ouvrant d'origine. C'est elle que vise la fin d'une
    regularisation (`activite`).
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  UUID ouverture,
  @Schema(
    description = """
    Echeance de l'activite : son debut plus la duree maximale d'une activite (13 heures ecoulees). Si aucune fin ne la
    termine avant, elle se termine automatiquement a cet instant : c'est une fin automatique, que le gestionnaire
    regularise.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Instant echeance
) {
  static RestActiviteEnCours from(ActiviteEnCours activite, AnnuaireDAtelier annuaire) {
    return new RestActiviteEnCours(
      RestOperateur.resolu(annuaire, activite.operateur()),
      RestPosteDeTravail.resolu(annuaire, activite.poste()),
      activite.categorie(),
      activite.depuis(),
      activite.ouverture().uuid(),
      activite.echeance().value()
    );
  }
}
