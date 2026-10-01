package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.ActiviteDeSupervision;
import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.EtatDActiviteDeSupervision;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

record RestActiviteDeSupervision(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID operateurId,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestElementDeSupervision element,
  RestPosteDeSupervision poste,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CategorieDActivite categorie,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant debut,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant echeance,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) EtatDActiviteDeSupervision etat,
  Instant finRetenue
) {
  static RestActiviteDeSupervision from(ActiviteDeSupervision activite) {
    var description = activite.description();
    return new RestActiviteDeSupervision(
      description.id().uuid(),
      description.operateur().uuid(),
      RestElementDeSupervision.from(description.element()),
      description.poste().map(RestPosteDeSupervision::from).orElse(null),
      description.categorie(),
      description.debut(),
      description.echeance(),
      activite.etat(),
      activite.finRetenue().orElse(null)
    );
  }
}
