package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.AmplitudeMaximale;
import com.glm.glmback.coutderevient.domain.SeuilDuCout;
import org.springframework.stereotype.Repository;

/**
 * La ligne est semee dans chaque schema d'entreprise : son absence est une corruption de la base, qui remonte telle
 * quelle plutot que d'etre masquee par une valeur codee ici.
 */
@Repository
class SeuilDuCoutDuParametrage implements SeuilDuCout {

  private final SpringDataSeuilDuCoutRepository parametrage;

  SeuilDuCoutDuParametrage(SpringDataSeuilDuCoutRepository parametrage) {
    this.parametrage = parametrage;
  }

  @Override
  public AmplitudeMaximale amplitudeMaximale() {
    return parametrage.findById(SeuilDuCoutEntity.LIGNE_UNIQUE).orElseThrow().toDomain();
  }
}
