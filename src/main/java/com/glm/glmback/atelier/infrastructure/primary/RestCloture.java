package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.ClotureAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Optional;

@Schema(
  description = """
  La cloture d'un element engage.

  Elle ne fige rien pour le gestionnaire : regularisation, annulation et correction restent possibles ensuite, et la
  cloture elle-meme peut etre deplacee ou annulee.
  """
)
record RestCloture(@Schema(description = "Heure metier de la cloture. Absente, elle vaut l'instant present.") Instant dateDeSurvenue) {
  ClotureAEnregistrer toDomain(SuiviDAtelierId suivi, Auteur auteur) {
    return new ClotureAEnregistrer(suivi, auteur, Optional.ofNullable(dateDeSurvenue));
  }
}
