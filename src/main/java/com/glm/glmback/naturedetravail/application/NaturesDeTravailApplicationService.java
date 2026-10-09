package com.glm.glmback.naturedetravail.application;

import com.glm.glmback.naturedetravail.domain.LibelleDeNature;
import com.glm.glmback.naturedetravail.domain.NatureDeTravail;
import com.glm.glmback.naturedetravail.domain.NatureDeTravailId;
import com.glm.glmback.naturedetravail.domain.NatureDeTravailListee;
import com.glm.glmback.naturedetravail.domain.NatureDeTravailRepository;
import com.glm.glmback.naturedetravail.domain.NaturesDeTravailService;
import com.glm.glmback.naturedetravail.domain.NaturesEnUsage;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NaturesDeTravailApplicationService {

  private final NaturesDeTravailService natures;

  public NaturesDeTravailApplicationService(NatureDeTravailRepository repository, NaturesEnUsage usages) {
    this.natures = new NaturesDeTravailService(repository, usages);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public NatureDeTravail declare(LibelleDeNature libelle) {
    return natures.declare(libelle);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public NatureDeTravailListee renomme(NatureDeTravailId id, LibelleDeNature libelle) {
    return natures.renomme(id, libelle);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public void delete(NatureDeTravailId id) {
    natures.delete(id);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public Page<NatureDeTravailListee> list(Pageable pageable) {
    return natures.list(pageable);
  }
}
