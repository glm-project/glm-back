package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.AnomalieDePresence;
import com.glm.glmback.atelier.domain.TypeDAnomalie;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Schema(
  description = """
  Une journee de travail que le gestionnaire doit regarder. Rien n'est stocke : la ligne disparait des que la
  regularisation la resout.
  """
)
record RestAnomalieDePresence(
  @Schema(
    description = "JOURNEE_SANS_DEPART : abandonnee sans depart au-dela du seuil. AMPLITUDE_EXCESSIVE : fermee au-dela du seuil.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  TypeDAnomalie type,
  @Schema(description = "La journee concernee, a regulariser ou a corriger.", requiredMode = Schema.RequiredMode.REQUIRED) UUID journee,
  @Schema(description = "L'operateur, resolu au referentiel.", requiredMode = Schema.RequiredMode.REQUIRED) RestOperateur operateur,
  @Schema(description = "Arrivee de la journee.", requiredMode = Schema.RequiredMode.REQUIRED) Instant arrivee,
  @Schema(description = "Depart, absent d'une journee sans depart.") Instant depart,
  @Schema(description = "Amplitude de l'arrivee au depart, absente d'une journee sans depart.", example = "PT16H") Duration amplitude
) {
  static RestAnomalieDePresence from(AnomalieDePresence anomalie, AnnuaireDAtelier annuaire) {
    return new RestAnomalieDePresence(
      anomalie.type(),
      anomalie.journee().id().uuid(),
      RestOperateur.resolu(annuaire, anomalie.journee().operateur()),
      anomalie.arrivee(),
      anomalie.depart().orElse(null),
      anomalie.amplitude().orElse(null)
    );
  }
}
