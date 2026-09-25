package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.AmplitudeMaximale;
import com.glm.glmback.atelier.domain.SeuilDAmplitude;
import org.springframework.stereotype.Repository;

/**
 * La ligne est semee dans chaque schema d'entreprise : son absence est une corruption de la base, qui remonte telle
 * quelle plutot que d'etre masquee par une valeur codee ici.
 */
@Repository
class SeuilDAmplitudeDuParametrage implements SeuilDAmplitude {

  private final SpringDataSeuilDAmplitudeRepository parametrage;

  SeuilDAmplitudeDuParametrage(SpringDataSeuilDAmplitudeRepository parametrage) {
    this.parametrage = parametrage;
  }

  @Override
  public AmplitudeMaximale amplitudeMaximale() {
    return parametrage.findById(SeuilDAmplitudeEntity.LIGNE_UNIQUE).orElseThrow().toDomain();
  }
}
