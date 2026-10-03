package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

record RestActiviteDuDossier(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID evenement,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID activite,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID operateurId,
  RestOperateur operateur,
  UUID posteId,
  RestPosteDeTravail poste,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CategorieDActivite categorie,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant debut,
  Instant fin,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = { "A_RESOUDRE", "EN_COURS", "TERMINEE", "ECHUE" }) String etat,
  String duree
) {
  static RestActiviteDuDossier from(IntervalleDActivite intervalle, AnnuaireDAtelier annuaire) {
    return new RestActiviteDuDossier(
      intervalle.evenement().uuid(),
      intervalle.activite().uuid(),
      intervalle.operateur().uuid(),
      RestOperateur.resolu(annuaire, intervalle.operateur()),
      intervalle.poste().map(PosteDeTravailId::uuid).orElse(null),
      RestPosteDeTravail.resolu(annuaire, intervalle.poste()),
      intervalle.categorie(),
      intervalle.debut(),
      intervalle.fin().orElse(null),
      intervalle.aResoudre()
        ? "A_RESOUDRE"
        : intervalle.fin().isPresent()
          ? intervalle.finAutomatique()
            ? "ECHUE"
            : "TERMINEE"
          : "EN_COURS",
      intervalle
        .fin()
        .map(fin -> Duration.between(intervalle.debut(), fin).toString())
        .orElse(null)
    );
  }
}
