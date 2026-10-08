package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.MontantTotal;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Montant total, deja au centime.")
record RestMontantDuCout(
  @Schema(description = "Montant.", example = "90.00", requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal valeur
) {
  static RestMontantDuCout from(MontantTotal total) {
    return new RestMontantDuCout(total.valeur().value());
  }
}
