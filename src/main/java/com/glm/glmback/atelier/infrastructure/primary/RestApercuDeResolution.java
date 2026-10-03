package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.application.ApercuDeResolution;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  description = "L'acte exact et ses consequences autoritaires, sans ecriture. La reference opaque est liee a la commande, l'adresse, la revision et l'identite authentifiee."
)
record RestApercuDeResolution(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID commande,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestAdresseDossierConflit adresse,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long revision,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant evaluation,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant expireLe,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reference,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestActeDeResolution acte,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestDossierConflit avant,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestDossierConflit apres
) {
  static RestApercuDeResolution from(ApercuDeResolution apercu, AnnuaireDAtelier avant, AnnuaireDAtelier apres) {
    var preuve = apercu.reference().preuve();
    return new RestApercuDeResolution(
      preuve.commande(),
      new RestAdresseDossierConflit(preuve.adresse().suivi().uuid(), preuve.adresse().pointage().uuid()),
      preuve.revision().value(),
      preuve.evaluation(),
      preuve.expireLe(),
      apercu.reference().opaque(),
      RestActeDeResolution.from(preuve.acte()),
      RestDossierConflit.from(apercu.avant(), avant),
      RestDossierConflit.from(apercu.apres(), apres)
    );
  }
}
