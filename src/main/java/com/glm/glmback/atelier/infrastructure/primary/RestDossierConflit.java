package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.EtatDAdresseDossier;
import com.glm.glmback.atelier.domain.LectureDossierConflit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(
  description = "Un dossier ancre dans le journal immutable, avec revision et interpretation a un meme instant. Les activites concernees restent presentes dans le resultat d un acte meme quand l ancre est annulee."
)
record RestDossierConflit(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EtatDAdresseDossier kind,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestAdresseDossierConflit adresse,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long revision,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant evaluation,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestSuiviDAtelier suivi,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RestDiagnosticDeConflit> diagnostics,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RestActiviteDuDossier> activites
) {
  static RestDossierConflit from(LectureDossierConflit dossier, AnnuaireDAtelier annuaire) {
    return new RestDossierConflit(
      dossier.kind(),
      new RestAdresseDossierConflit(dossier.adresse().suivi().uuid(), dossier.adresse().pointage().uuid()),
      dossier.lecture().suivi().revision().value(),
      dossier.lecture().evaluation(),
      RestSuiviDAtelier.from(dossier.lecture(), annuaire),
      dossier.diagnostics().stream().map(RestDiagnosticDeConflit::from).toList(),
      dossier
        .activites()
        .stream()
        .map(intervalle -> RestActiviteDuDossier.from(intervalle, annuaire))
        .toList()
    );
  }
}
