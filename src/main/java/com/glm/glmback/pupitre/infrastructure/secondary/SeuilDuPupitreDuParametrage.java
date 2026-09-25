package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.AmplitudeMaximale;
import com.glm.glmback.pupitre.domain.SeuilDuPupitre;
import org.springframework.stereotype.Repository;

/**
 * La ligne est semee dans chaque schema d'entreprise : son absence est une corruption de la base, qui remonte telle
 * quelle plutot que d'etre masquee par une valeur codee ici.
 */
@Repository
class SeuilDuPupitreDuParametrage implements SeuilDuPupitre {

  private final SpringDataSeuilDuPupitreRepository parametrage;

  SeuilDuPupitreDuParametrage(SpringDataSeuilDuPupitreRepository parametrage) {
    this.parametrage = parametrage;
  }

  @Override
  public AmplitudeMaximale amplitudeMaximale() {
    return parametrage.findById(SeuilDuPupitreEntity.LIGNE_UNIQUE).orElseThrow().toDomain();
  }
}
