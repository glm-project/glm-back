package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.glm.glmback.atelier.application.gestionanomalies.ApercuDeResolution;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  description = "L'acte exact et ses consequences autoritaires, sans ecriture. La proposition explicite est recalculee a la confirmation."
)
record RestApercuDeResolution(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID commande,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestAdresseDossierAnomalie adresse,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long revision,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant evaluation,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String empreinteConsequences,
  UUID evenement,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestActeDeResolution acte,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestDossierAnomalie avant,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestDossierAnomalie apres
) {
  static RestApercuDeResolution from(ApercuDeResolution apercu, AnnuaireDAtelier avant, AnnuaireDAtelier apres) {
    var proposition = apercu.proposition();
    return new RestApercuDeResolution(
      proposition.commande(),
      new RestAdresseDossierAnomalie(proposition.adresse().suivi().uuid(), proposition.adresse().pointage().uuid()),
      proposition.revision().value(),
      apercu.evaluation(),
      proposition.empreinteConsequences(),
      proposition
        .evenement()
        .map(id -> id.uuid())
        .orElse(null),
      RestActeDeResolution.from(proposition.acte()),
      RestDossierAnomalie.from(apercu.avant(), avant),
      RestDossierAnomalie.from(apercu.apres(), apres)
    );
  }
}
