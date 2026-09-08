package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.Periode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Un intervalle ferme des deux bouts.")
record RestPeriode(
  @Schema(description = "Debut de la periode.") Instant debut,
  @Schema(description = "Fin de la periode. Un travail encore en cours est arrete a l'instant de la lecture.") Instant fin
) {
  static RestPeriode from(Periode periode) {
    return new RestPeriode(periode.debut(), periode.fin());
  }
}
