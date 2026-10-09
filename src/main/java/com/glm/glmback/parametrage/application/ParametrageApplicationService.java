package com.glm.glmback.parametrage.application;

import com.glm.glmback.parametrage.domain.DureeMaxDActivite;
import com.glm.glmback.parametrage.domain.Parametrage;
import com.glm.glmback.parametrage.domain.ParametrageRepository;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParametrageApplicationService {

  private final ParametrageRepository repository;

  public ParametrageApplicationService(ParametrageRepository repository) {
    this.repository = repository;
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public Parametrage get() {
    return repository.get();
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public Parametrage fixeLaDureeMaxDActivite(DureeMaxDActivite duree) {
    return repository.update(repository.get().fixeLaDureeMaxDActivite(duree));
  }
}
