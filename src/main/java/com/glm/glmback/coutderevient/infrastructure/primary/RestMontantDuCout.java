package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.Montant;
import com.glm.glmback.coutderevient.domain.MontantTotal;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Total complet chiffre, ou incomplet sans aucun chiffre partiel.")
record RestMontantDuCout(
  @Schema(description = "Valeur certaine, absente si incomplet.", example = "90.00") BigDecimal valeur,
  @Schema(description = "Vrai si ce total est entierement determine.", requiredMode = Schema.RequiredMode.REQUIRED) boolean complete
) {
  static RestMontantDuCout from(MontantTotal total) {
    return new RestMontantDuCout(total.valeur().map(Montant::value).orElse(null), total.complete());
  }
}
