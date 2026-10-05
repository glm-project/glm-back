package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.glm.glmback.atelier.domain.DiagnosticDeConflit;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.RaisonDuConflit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

record RestDiagnosticDeConflit(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID pointage,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestCibleDuConflit cible,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RaisonDuConflit raison
) {
  static RestDiagnosticDeConflit from(DiagnosticDeConflit diagnostic) {
    var cible = diagnostic.cible();
    return new RestDiagnosticDeConflit(
      diagnostic.pointage().uuid(),
      new RestCibleDuConflit(
        cible.activite().uuid(),
        cible.ouvrant().map(EvenementDAtelierId::uuid).orElse(null),
        cible.termineePar().map(EvenementDAtelierId::uuid).orElse(null)
      ),
      diagnostic.raison()
    );
  }

  record RestCibleDuConflit(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID activite, UUID ouvrant, UUID termineePar) {}
}
