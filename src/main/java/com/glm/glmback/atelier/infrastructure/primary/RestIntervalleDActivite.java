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
  Du temps passe sur un element, deduit du journal et jamais stocke.

  Le temps effectif est l'intersection des intervalles bruts avec les fenetres de presence de l'operateur : c'est
  pourquoi une pause de midi produit deux intervalles la ou l'operateur n'a pointe qu'un debut.
  """
)
record RestIntervalleDActivite(
  @Schema(description = "Evenement qui a ouvert l'intervalle.") UUID evenement,
  @Schema(description = "Operateur concerne.") RestOperateur operateur,
  @Schema(description = "Poste de travail, facultatif.") RestPosteDeTravail poste,
  @Schema(description = "Nature de l'operation, facultative.") String nature,
  @Schema(description = "TRAVAIL ou NON_CONFORMITE.") CategorieDActivite categorie,
  @Schema(description = "Debut de l'intervalle.") Instant debut,
  @Schema(description = "Fin de l'intervalle, absente s'il est encore en cours.") Instant fin,
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
      RestOperateur.resolu(annuaire, intervalle.operateur()),
      RestPosteDeTravail.resolu(annuaire, intervalle.poste()),
      intervalle.nature().map(NatureDOperation::value).orElse(null),
      intervalle.categorie(),
      intervalle.debut(),
      intervalle.fin().orElse(null),
      intervalle.presume()
    );
  }
}
