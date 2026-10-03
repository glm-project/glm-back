package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.DiagnosticDeConflit;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.RaisonDuConflit;
import java.util.UUID;

record RestDiagnosticDeConflit(UUID pointage, RestCibleDuConflit cible, RaisonDuConflit raison) {
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

  record RestCibleDuConflit(UUID activite, UUID ouvrant, UUID termineePar) {}
}
