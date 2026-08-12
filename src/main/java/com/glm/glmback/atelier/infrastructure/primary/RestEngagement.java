package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.EngagementAEnregistrer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Mise d'un element de fabrication en atelier.")
record RestEngagement(
  @Schema(description = "Identifiant de l'element de fabrication a engager.", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  UUID element
) {
  EngagementAEnregistrer toDomain(Auteur auteur) {
    return new EngagementAEnregistrer(new ElementEngageId(element), auteur);
  }
}
