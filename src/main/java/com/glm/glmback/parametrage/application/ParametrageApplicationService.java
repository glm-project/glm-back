package com.glm.glmback.parametrage.application;

import com.glm.glmback.parametrage.domain.AmplitudeMaximale;
import com.glm.glmback.parametrage.domain.Auteur;
import com.glm.glmback.parametrage.domain.Parametrage;
import com.glm.glmback.parametrage.domain.ParametrageRepository;
import com.glm.glmback.parametrage.domain.ParametrageService;
import com.glm.glmback.shared.time.domain.Clock;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParametrageApplicationService {

  private final ParametrageService parametrage;

  public ParametrageApplicationService(ParametrageRepository repository, Clock clock) {
    this.parametrage = new ParametrageService(repository, clock);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public Parametrage get() {
    return parametrage.get();
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public Parametrage fixeLAmplitudeMaximale(AmplitudeMaximale amplitude, Auteur auteur) {
    return parametrage.fixeLAmplitudeMaximale(amplitude, auteur);
  }
}
