package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.AmplitudeMaximale;
import com.glm.glmback.syntheseheures.domain.SeuilDAmplitude;
import org.springframework.stereotype.Repository;

/**
 * La ligne est semee dans chaque schema d'entreprise : son absence est une corruption de la base, qui remonte telle
 * quelle plutot que d'etre masquee par une valeur codee ici.
 */
@Repository
class SeuilDeLaSynthese implements SeuilDAmplitude {

  private final SpringDataSeuilDeLaSyntheseRepository parametrage;

  SeuilDeLaSynthese(SpringDataSeuilDeLaSyntheseRepository parametrage) {
    this.parametrage = parametrage;
  }

  @Override
  public AmplitudeMaximale amplitudeMaximale() {
    return parametrage.findById(SeuilDeLaSyntheseEntity.LIGNE_UNIQUE).orElseThrow().toDomain();
  }
}
