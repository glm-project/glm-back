package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.EtatDAdresseDossier;
import com.glm.glmback.atelier.domain.LectureDossierConflit;
import java.time.Instant;
import java.util.List;

record RestDossierConflit(
  EtatDAdresseDossier kind,
  RestAdresseDossierConflit adresse,
  long revision,
  Instant evaluation,
  RestSuiviDAtelier suivi,
  List<RestDiagnosticDeConflit> diagnostics
) {
  static RestDossierConflit from(LectureDossierConflit dossier, AnnuaireDAtelier annuaire) {
    return new RestDossierConflit(
      dossier.kind(),
      new RestAdresseDossierConflit(dossier.adresse().suivi().uuid(), dossier.adresse().pointage().uuid()),
      dossier.lecture().suivi().revision().value(),
      dossier.lecture().evaluation(),
      RestSuiviDAtelier.from(dossier.lecture(), annuaire),
      dossier.diagnostics().stream().map(RestDiagnosticDeConflit::from).toList()
    );
  }
}
