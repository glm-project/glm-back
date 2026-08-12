package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.IntervalleDActivite;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.PosteDeTravail;
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
  @Schema(description = "Operateur concerne.", example = "dupont") String operateur,
  @Schema(description = "Poste de travail, facultatif.", example = "fraiseuse-1") String poste,
  @Schema(description = "Nature de l'operation, facultative.") String nature,
  @Schema(description = "TRAVAIL ou NON_CONFORMITE.") CategorieDActivite categorie,
  @Schema(description = "Debut de l'intervalle.") Instant debut,
  @Schema(description = "Fin de l'intervalle, absente s'il est encore en cours.") Instant fin
) {
  static RestIntervalleDActivite from(IntervalleDActivite intervalle) {
    return new RestIntervalleDActivite(
      intervalle.evenement().uuid(),
      intervalle.operateur().value(),
      intervalle.poste().map(PosteDeTravail::value).orElse(null),
      intervalle.nature().map(NatureDOperation::value).orElse(null),
      intervalle.categorie(),
      intervalle.debut(),
      intervalle.fin().orElse(null)
    );
  }
}
