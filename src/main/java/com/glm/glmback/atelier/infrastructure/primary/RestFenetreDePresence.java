package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.FenetreDePresence;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(
  description = """
  Un intervalle pendant lequel l'operateur etait present et non en pause.

  C'est la matiere du temps effectif : une pause de midi scinde la journee en deux fenetres, un depart referme la
  derniere. Une fenetre sans `fin` est encore ouverte.
  """
)
record RestFenetreDePresence(
  @Schema(description = "Debut de la fenetre.") Instant debut,
  @Schema(description = "Fin de la fenetre, absente si l'operateur est toujours present.") Instant fin
) {
  static RestFenetreDePresence from(FenetreDePresence fenetre) {
    return new RestFenetreDePresence(fenetre.debut(), fenetre.fin().orElse(null));
  }
}
