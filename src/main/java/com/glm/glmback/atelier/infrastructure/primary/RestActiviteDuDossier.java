package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.*;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

record RestActiviteDuDossier(
  UUID evenement,
  UUID activite,
  UUID operateurId,
  RestOperateur operateur,
  UUID posteId,
  RestPosteDeTravail poste,
  CategorieDActivite categorie,
  Instant debut,
  Instant fin,
  String etat,
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
