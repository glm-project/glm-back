package com.glm.glmback.parametrage.domain;

import com.glm.glmback.shared.time.domain.Clock;

public class ParametrageService {

  private final ParametrageRepository repository;
  private final Clock clock;

  public ParametrageService(ParametrageRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  public Parametrage get() {
    return repository.get();
  }

  public Parametrage fixeLAmplitudeMaximale(AmplitudeMaximale amplitude, Auteur auteur) {
    Parametrage fixe = repository.get().fixeLAmplitudeMaximale(amplitude, new Modification(auteur, clock.now()));
    repository.update(fixe);

    return fixe;
  }
}
