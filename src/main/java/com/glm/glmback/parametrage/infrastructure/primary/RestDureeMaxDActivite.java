package com.glm.glmback.parametrage.infrastructure.primary;

import com.glm.glmback.parametrage.domain.DureeMaxDActivite;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;

@Schema(description = "La duree max d'une activite que l'entreprise fixe.")
record RestDureeMaxDActivite(
  @Schema(
    description = "Duree au bout de laquelle une activite que rien n'a terminee se termine automatiquement, en ISO-8601 : d'une heure (PT1H) a vingt-quatre heures (PT24H), bornes comprises.",
    example = "PT10H",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  @DurationMin(hours = 1)
  @DurationMax(hours = 24)
  Duration dureeMaxDActivite
) {
  DureeMaxDActivite toDomain() {
    return new DureeMaxDActivite(dureeMaxDActivite);
  }
}
