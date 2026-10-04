package com.glm.glmback.atelier.infrastructure.primary.gestionconflits;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.gestionconflits.EtatDAdresseDossier;
import com.glm.glmback.atelier.domain.gestionconflits.LectureDossierConflit;
import com.glm.glmback.atelier.infrastructure.primary.RestSuiviDAtelier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(
  description = "Un dossier ancre dans le journal immutable, avec revision et interpretation a un meme instant. Les activites concernees restent presentes dans le resultat d un acte meme quand l ancre est annulee."
)
record RestDossierConflit(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EtatDAdresseDossier kind,
  @Schema(
    requiredMode = Schema.RequiredMode.REQUIRED,
    description = "Le perimetre concerne reste en conflit dans l interpretation du domaine, meme sans intervalle d activite ou avec une ancre annulee."
  )
  boolean enConflit,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestAdresseDossierConflit adresse,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long revision,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant evaluation,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestSuiviDAtelier suivi,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RestDiagnosticDeConflit> diagnostics,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RestActiviteDuDossier> activites,
  RestSequenceDuDossier sequence,
  @Schema(description = "Le perimetre conserve des faits et activites concernes, y compris apres resolution ou annulation de l ancre.")
  RestSequenceDuDossier perimetre,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<RestChoixDeResolution> choix,
  @Schema(
    requiredMode = Schema.RequiredMode.REQUIRED,
    description = "Les autres sequences encore en conflit de ce suivi, par ancre active ; aucune redirection implicite."
  )
  List<RestConflitEnListe> continuations
) {
  static RestDossierConflit from(LectureDossierConflit dossier, AnnuaireDAtelier annuaire) {
    return new RestDossierConflit(
      dossier.kind(),
      dossier.enConflit(),
      new RestAdresseDossierConflit(dossier.adresse().suivi().uuid(), dossier.adresse().pointage().uuid()),
      dossier.lecture().suivi().revision().value(),
      dossier.lecture().evaluation(),
      RestSuiviDAtelier.from(dossier.lecture(), annuaire),
      dossier.diagnostics().stream().map(RestDiagnosticDeConflit::from).toList(),
      dossier
        .activites()
        .stream()
        .map(intervalle -> RestActiviteDuDossier.from(intervalle, annuaire))
        .toList(),
      RestSequenceDuDossier.from(dossier, annuaire),
      RestSequenceDuDossier.perimetre(dossier, annuaire),
      dossier
        .choix()
        .stream()
        .map(choix -> RestChoixDeResolution.from(choix, dossier))
        .toList(),
      dossier
        .continuations()
        .stream()
        .map(ligne -> RestConflitEnListe.from(ligne, annuaire))
        .toList()
    );
  }
}
