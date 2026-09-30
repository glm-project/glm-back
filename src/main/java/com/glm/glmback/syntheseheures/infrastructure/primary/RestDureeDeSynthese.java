package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.DureeTotale;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;

@Schema(description = "Total complet chiffre, ou incomplet sans valeur, jamais une somme partielle ni zero par defaut.")
record RestDureeDeSynthese(
  @Schema(description = "Duree certaine, absente si le total est incomplet.", example = "PT2H") Duration valeur,
  @Schema(description = "Vrai si aucune activite a resoudre ne contribue a ce total.", requiredMode = Schema.RequiredMode.REQUIRED)
  boolean complete
) {
  static RestDureeDeSynthese from(DureeTotale duree) {
    return new RestDureeDeSynthese(duree.valeur().orElse(null), duree.complete());
  }
}
