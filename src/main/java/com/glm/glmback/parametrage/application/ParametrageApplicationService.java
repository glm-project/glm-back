package com.glm.glmback.parametrage.application;

import com.glm.glmback.parametrage.domain.DecodeurDImage;
import com.glm.glmback.parametrage.domain.DepotDeLogo;
import com.glm.glmback.parametrage.domain.DureeMaxDActivite;
import com.glm.glmback.parametrage.domain.Logo;
import com.glm.glmback.parametrage.domain.LogoRepository;
import com.glm.glmback.parametrage.domain.Parametrage;
import com.glm.glmback.parametrage.domain.ParametrageRepository;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParametrageApplicationService {

  private final ParametrageRepository repository;
  private final DepotDeLogo depot;

  public ParametrageApplicationService(ParametrageRepository repository, DecodeurDImage decodeur, LogoRepository logos) {
    this.repository = repository;
    this.depot = new DepotDeLogo(decodeur, logos);
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

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public Logo deposeLeLogo(byte[] contenu) {
    return depot.depose(contenu);
  }
}
