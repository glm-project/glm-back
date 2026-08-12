package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.Periode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Un intervalle de temps borne a ses deux extremites.")
record RestPeriode(@Schema(description = "Debut inclus.") Instant debut, @Schema(description = "Fin incluse.") Instant fin) {
  static RestPeriode from(Periode periode) {
    return new RestPeriode(periode.debut(), periode.fin());
  }
}
