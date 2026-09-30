package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.TempsPasse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
  description = """
  Le temps passe, separe en bon travail et en reprise de non conformite.

  La reprise d'une piece ratee se compte a part sur le meme element. Ses tarifs sont ceux de son propre fait
  ouvrant actif, captures par atelier.
  """
)
record RestTempsPasse(
  @Schema(description = "Duree ISO-8601 du bon travail.") RestDureeDuCout travail,
  @Schema(description = "Duree ISO-8601 passee en non conformite.") RestDureeDuCout nonConformite,
  @Schema(description = "Somme des deux.") RestDureeDuCout total
) {
  static RestTempsPasse from(TempsPasse temps) {
    return new RestTempsPasse(
      RestDureeDuCout.from(temps.travail()),
      RestDureeDuCout.from(temps.nonConformite()),
      RestDureeDuCout.from(temps.total())
    );
  }
}
