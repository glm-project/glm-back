package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.gestionanomalies.LectureDossierAnomalie;
import com.glm.glmback.atelier.infrastructure.primary.RestEvenementDAtelier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(
  description = "Le dossier d'une fin automatique non regularisee : l'activite echue et les pointages de sa cle, a l'instant d'evaluation."
)
record RestDossierAnomalie(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestAdresseDossierAnomalie adresse,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long revision,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant evaluation,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestActiviteDuDossier activite,
  @Schema(
    requiredMode = Schema.RequiredMode.REQUIRED,
    description = "Les pointages du suivi qui portent la cle de l'activite, operateur et poste : du plus ancien au plus recent."
  )
  List<RestEvenementDAtelier> pointages
) {
  static RestDossierAnomalie from(LectureDossierAnomalie dossier, AnnuaireDAtelier annuaire) {
    return new RestDossierAnomalie(
      new RestAdresseDossierAnomalie(dossier.adresse().suivi().uuid(), dossier.adresse().pointage().uuid()),
      dossier.lecture().suivi().revision().value(),
      dossier.lecture().evaluation(),
      RestActiviteDuDossier.from(dossier.activite(), annuaire),
      dossier
        .pointages()
        .stream()
        .map(pointage -> RestEvenementDAtelier.from(pointage, annuaire))
        .toList()
    );
  }
}
