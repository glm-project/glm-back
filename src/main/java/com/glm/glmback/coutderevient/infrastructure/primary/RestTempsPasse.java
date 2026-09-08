package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.TempsPasse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;

@Schema(
  description = """
  Le temps passe, separe en bon travail et en reprise de non conformite.

  Une piece ratee se refait sur le meme element et au meme tarif, mais comptee a part : c'est ce que le client veut
  savoir a la cloture.
  """
)
record RestTempsPasse(
  @Schema(description = "Duree ISO-8601 du bon travail.", example = "PT2H") Duration travail,
  @Schema(description = "Duree ISO-8601 passee en non conformite.", example = "PT30M") Duration nonConformite,
  @Schema(description = "Somme des deux.", example = "PT2H30M") Duration total
) {
  static RestTempsPasse from(TempsPasse temps) {
    return new RestTempsPasse(temps.travail(), temps.nonConformite(), temps.total());
  }
}
