package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.DureeTotale;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;

@Schema(description = "Total complet chiffre, ou incomplet sans aucun chiffre partiel.")
record RestDureeDuCout(
  @Schema(description = "Valeur certaine ISO-8601, absente si incomplet.", example = "PT2H") Duration valeur,
  @Schema(description = "Vrai si ce total est entierement determine.", requiredMode = Schema.RequiredMode.REQUIRED) boolean complete
) {
  static RestDureeDuCout from(DureeTotale total) {
    return new RestDureeDuCout(total.valeur().orElse(null), total.complete());
  }
}
