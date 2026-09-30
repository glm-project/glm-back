package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.Periode;
import com.glm.glmback.coutderevient.domain.Plage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "RestPeriodeDuCout", description = "Bornes des activites comptabilisees ; fin absente si aucune fin certaine.")
record RestPeriode(
  @Schema(description = "Debut de la periode.") Instant debut,
  @Schema(description = "Derniere fin certaine, absente si aucune activite de cette nature n'est terminee.") Instant fin
) {
  static RestPeriode from(Plage plage) {
    return new RestPeriode(plage.debut(), plage.fin().orElse(null));
  }

  static RestPeriode from(Periode periode) {
    return new RestPeriode(periode.debut(), periode.fin());
  }
}
