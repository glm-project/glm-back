package com.glm.glmback.parametrage.infrastructure.primary;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.glm.glmback.parametrage.domain.AmplitudeMaximale;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;

@Schema(description = "La nouvelle amplitude maximale d'une journee de travail.")
record RestAmplitudeMaximale(
  @Schema(
    description = "Duree ISO 8601, a la minute, strictement comprise entre 0 et 24 h.",
    example = "PT13H",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  @DurationMin(minutes = 1)
  @DurationMax(hours = 24, inclusive = false)
  Duration valeur
) {
  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = "l'amplitude maximale se compte a la minute")
  boolean isALaMinute() {
    return valeur == null || (valeur.toSecondsPart() == 0 && valeur.toNanosPart() == 0);
  }

  AmplitudeMaximale toDomain() {
    return new AmplitudeMaximale(valeur);
  }
}
