package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.Periode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "RestPeriodeDuCout", description = "Periode terminee effectivement ou automatiquement.")
record RestPeriode(
  @Schema(description = "Debut de la periode.") Instant debut,
  @Schema(description = "Fin de la periode terminee.") Instant fin
) {
  static RestPeriode from(Periode periode) {
    return new RestPeriode(periode.debut(), periode.fin());
  }
}
