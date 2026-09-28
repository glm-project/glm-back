package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.NatureDOperation;
import com.glm.glmback.syntheseheures.domain.PosteDeLElement;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
  name = "RestPosteDeLElementDeLaSynthese",
  description = "Un poste sur lequel l'element a ete travaille dans la semaine, avec la nature figee a la saisie."
)
record RestPosteDeLElementDeLaSynthese(
  @Schema(description = "Le poste pointe.", requiredMode = Schema.RequiredMode.REQUIRED) RestPosteDeLaSynthese poste,
  @Schema(description = "La nature de l'operation, figee a la saisie depuis le poste.", example = "Fraisage") String nature
) {
  static RestPosteDeLElementDeLaSynthese from(PosteDeLElement poste) {
    return new RestPosteDeLElementDeLaSynthese(
      RestPosteDeLaSynthese.from(poste.poste()),
      poste.nature().map(NatureDOperation::value).orElse(null)
    );
  }
}
