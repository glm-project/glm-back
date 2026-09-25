package com.glm.glmback.feuilledetemps.infrastructure.primary;

import com.glm.glmback.feuilledetemps.domain.Plage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(
  description = """
  Un intervalle de presence, deja ramene au jour qui le porte.

  Une plage sans fin n'est pas une anomalie : l'operateur n'a pas encore pointe son depart.
  """
)
record RestPlage(
  @Schema(description = "Debut de la plage.", example = "2026-05-11T06:00:00Z") Instant debut,
  @Schema(description = "Fin de la plage, absente tant que l'operateur n'est pas parti.") Instant fin,
  @Schema(
    description = """
    Vrai si la plage repose sur une fin de journee presumee : l'operateur n'a pas pointe son depart, et sa journee,
    abandonnee au-dela de l'amplitude maximale, a ete fermee a son dernier fait connu. A confirmer par une
    regularisation du depart.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  boolean presumee
) {
  static RestPlage from(Plage plage) {
    return new RestPlage(plage.debut(), plage.fin().orElse(null), plage.presumee());
  }
}
