package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.PosteConnu;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(name = "RestPosteDeLaSynthese", description = "Un poste de travail, nomme par le referentiel a la lecture.")
record RestPosteDeLaSynthese(
  @Schema(description = "Identifiant du poste dans le referentiel.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Libelle du poste, relu a chaque appel.", example = "DMU 50", requiredMode = Schema.RequiredMode.REQUIRED)
  String libelle
) {
  static RestPosteDeLaSynthese from(PosteConnu poste) {
    return new RestPosteDeLaSynthese(poste.id().uuid(), poste.libelle().value());
  }
}
