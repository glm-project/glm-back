package com.glm.glmback.parametrage.application;

import com.glm.glmback.parametrage.domain.DecodeurDImage;
import com.glm.glmback.parametrage.domain.DepotDeLogo;
import com.glm.glmback.parametrage.domain.DureeMaxDActivite;
import com.glm.glmback.parametrage.domain.LectureDuLogo;
import com.glm.glmback.parametrage.domain.Logo;
import com.glm.glmback.parametrage.domain.LogoRepository;
import com.glm.glmback.parametrage.domain.ParametrageRepository;
import com.glm.glmback.parametrage.domain.VersionDuLogo;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParametrageApplicationService {

  private final ParametrageRepository repository;
  private final LogoRepository logos;
  private final DepotDeLogo depot;
  private final LectureDuLogo lecture;

  public ParametrageApplicationService(ParametrageRepository repository, DecodeurDImage decodeur, LogoRepository logos) {
    this.repository = repository;
    this.logos = logos;
    this.depot = new DepotDeLogo(decodeur, logos);
    this.lecture = new LectureDuLogo(logos);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public ParametrageLu get() {
    return new ParametrageLu(repository.get(), logos.version());
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public ParametrageLu fixeLaDureeMaxDActivite(DureeMaxDActivite duree) {
    return new ParametrageLu(repository.update(repository.get().fixeLaDureeMaxDActivite(duree)), logos.version());
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public Logo deposeLeLogo(byte[] contenu) {
    return depot.depose(contenu);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public Logo logo(VersionDuLogo version) {
    return lecture.enVersion(version);
  }
}
