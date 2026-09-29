package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.IntervalleDActivite;
import com.glm.glmback.atelier.domain.NatureDOperation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  description = """
  Du temps passe sur un element, deduit du journal et jamais stocke, tel qu'il se lit a l'instant de la lecture.

  Le temps effectif est l'intersection des intervalles bruts avec les fenetres de presence de l'operateur : c'est
  pourquoi un depart referme un intervalle que l'operateur n'a jamais arrete. Une activite que rien n'a terminee avant
  son echeance, son debut plus 13 heures, y est terminee automatiquement a cette echeance, avec une anomalie.
  """
)
record RestIntervalleDActivite(
  @Schema(description = "Evenement qui a ouvert l'intervalle.") UUID evenement,
  @Schema(
    description = "Activite de l'intervalle : l'identifiant de son pointage ouvrant d'origine, celui que vise une fin ou une transition.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  UUID activite,
  @Schema(description = "Operateur concerne.") RestOperateur operateur,
  @Schema(description = "Poste de travail, facultatif.") RestPosteDeTravail poste,
  @Schema(description = "Nature de l'operation, facultative.") String nature,
  @Schema(description = "TRAVAIL ou NON_CONFORMITE.") CategorieDActivite categorie,
  @Schema(description = "Debut de l'intervalle.") Instant debut,
  @Schema(
    description = """
    Fin de l'intervalle, absente s'il est encore en cours ou a resoudre. Pour une activite terminee automatiquement, son
    echeance.
    """
  )
  Instant fin,
  @Schema(
    description = """
    Vrai si l'activite de l'intervalle est terminee automatiquement a son echeance, faute de fin reelle : c'est une
    anomalie, qu'une fin pointee au plus tard a l'echeance, ou regularisee, retire au recalcul. Elle suit l'activite,
    meme quand la presence ramene l'intervalle en deca de son echeance.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  boolean finAutomatique,
  @Schema(
    description = """
    Vrai si l'activite de l'intervalle est a resoudre : des pointages contradictoires la concernent, dans une sequence
    en conflit. Elle n'est alors ni en cours ni terminee, n'a ni fin ni duree a compter, et son echeance ne la termine
    pas. Une correction ou une annulation du gestionnaire la rend de nouveau interpretable, au recalcul.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  boolean aResoudre,
  @Schema(
    description = """
    Vrai si l'intervalle repose sur une fin de journee presumee : l'operateur n'a pas pointe son depart, et sa
    journee, abandonnee au-dela de l'amplitude maximale, a ete fermee a son dernier fait connu. A confirmer par une
    regularisation du depart.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  boolean presume
) {
  static RestIntervalleDActivite from(IntervalleDActivite intervalle, AnnuaireDAtelier annuaire) {
    return new RestIntervalleDActivite(
      intervalle.evenement().uuid(),
      intervalle.activite().uuid(),
      RestOperateur.resolu(annuaire, intervalle.operateur()),
      RestPosteDeTravail.resolu(annuaire, intervalle.poste()),
      intervalle.nature().map(NatureDOperation::value).orElse(null),
      intervalle.categorie(),
      intervalle.debut(),
      intervalle.fin().orElse(null),
      intervalle.finAutomatique(),
      intervalle.aResoudre(),
      intervalle.presume()
    );
  }
}
