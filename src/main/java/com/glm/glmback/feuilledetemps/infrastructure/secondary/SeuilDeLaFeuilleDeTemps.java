package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import com.glm.glmback.feuilledetemps.domain.AmplitudeMaximale;
import com.glm.glmback.feuilledetemps.domain.SeuilDAmplitude;
import org.springframework.stereotype.Repository;

/**
 * La ligne est semee dans chaque schema d'entreprise : son absence est une corruption de la base, qui remonte telle
 * quelle plutot que d'etre masquee par une valeur codee ici.
 */
@Repository
class SeuilDeLaFeuilleDeTemps implements SeuilDAmplitude {

  private final SpringDataSeuilDeLaFeuilleDeTempsRepository parametrage;

  SeuilDeLaFeuilleDeTemps(SpringDataSeuilDeLaFeuilleDeTempsRepository parametrage) {
    this.parametrage = parametrage;
  }

  @Override
  public AmplitudeMaximale amplitudeMaximale() {
    return parametrage.findById(SeuilDeLaFeuilleDeTempsEntity.LIGNE_UNIQUE).orElseThrow().toDomain();
  }
}
