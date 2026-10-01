package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.FenetreDePresence;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(
  description = """
  Un intervalle pendant lequel l'operateur etait present, de son arrivee a son depart.

  C'est la matiere du temps effectif : un depart referme la fenetre, et avec elle le travail que l'operateur a oublie
  d'arreter. Une fenetre sans `fin` est encore ouverte.
  """
)
record RestFenetreDePresence(
  @Schema(description = "Debut de la fenetre.", requiredMode = Schema.RequiredMode.REQUIRED) Instant debut,
  @Schema(description = "Fin de la fenetre, absente si l'operateur est toujours present.") Instant fin
) {
  static RestFenetreDePresence from(FenetreDePresence fenetre) {
    return new RestFenetreDePresence(fenetre.debut(), fenetre.fin().orElse(null));
  }
}
