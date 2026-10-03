package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.DescriptionDActiviteDeSupervision;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

record RestDescriptionDActiviteDeSupervision(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID operateurId,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestElementDeSupervision element,
  RestPosteDeSupervision poste,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CategorieDActivite categorie,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant debut,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant echeance
) {
  static RestDescriptionDActiviteDeSupervision from(DescriptionDActiviteDeSupervision description) {
    return new RestDescriptionDActiviteDeSupervision(
      description.id().uuid(),
      description.operateur().uuid(),
      RestElementDeSupervision.from(description.element()),
      description.poste().map(RestPosteDeSupervision::from).orElse(null),
      description.categorie(),
      description.debut(),
      description.echeance().value()
    );
  }
}
