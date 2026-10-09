package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.Activite;
import com.glm.glmback.coutderevient.domain.AnnuaireDuCout;
import com.glm.glmback.coutderevient.domain.NatureDOperation;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RestActiviteCiteeDuCout", description = "Une autre activite du meme operateur, menee de front, tous elements confondus.")
record RestActiviteCitee(
  @Schema(description = "Element sur lequel portait l'activite, ce meme element compris.", requiredMode = Schema.RequiredMode.REQUIRED)
  RestElementCite element,
  @Schema(description = "Poste de l'activite, absent sans poste.") RestPosteCite poste,
  @Schema(description = "Nature de l'operation, absente sans poste.", example = "Fraisage") String nature
) {
  static RestActiviteCitee from(Activite activite, AnnuaireDuCout annuaire) {
    return new RestActiviteCitee(
      RestElementCite.from(activite.element(), annuaire),
      activite
        .poste()
        .map(poste -> RestPosteCite.from(poste, annuaire))
        .orElse(null),
      activite.nature().map(NatureDOperation::value).orElse(null)
    );
  }
}
