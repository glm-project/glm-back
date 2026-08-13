package com.glm.glmback.postedetravail.application;

import com.glm.glmback.postedetravail.domain.NatureDeTravail;
import com.glm.glmback.postedetravail.domain.PosteDeTravail;
import com.glm.glmback.postedetravail.domain.PosteDeTravailACreer;
import com.glm.glmback.postedetravail.domain.PosteDeTravailAModifier;
import com.glm.glmback.postedetravail.domain.PosteDeTravailId;
import com.glm.glmback.postedetravail.domain.PosteDeTravailRepository;
import com.glm.glmback.postedetravail.domain.PostesDeTravailService;
import com.glm.glmback.postedetravail.domain.PostesEnUsage;
import com.glm.glmback.postedetravail.domain.PostesPointes;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostesDeTravailApplicationService {

  private final PostesDeTravailService postesDeTravail;

  public PostesDeTravailApplicationService(PosteDeTravailRepository repository, PostesEnUsage usages, PostesPointes pointages) {
    this.postesDeTravail = new PostesDeTravailService(repository, usages, pointages);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public PosteDeTravail create(PosteDeTravailACreer aCreer) {
    return postesDeTravail.create(aCreer);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public PosteDeTravail get(PosteDeTravailId id) {
    return postesDeTravail.get(id);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public Page<PosteDeTravail> list(Optional<NatureDeTravail> nature, Pageable pageable) {
    return postesDeTravail.list(nature, pageable);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public PosteDeTravail update(PosteDeTravailAModifier aModifier) {
    return postesDeTravail.update(aModifier);
  }

  @Secured("ROLE_GESTIONNAIRE")
  @Transactional
  public void delete(PosteDeTravailId id) {
    postesDeTravail.delete(id);
  }
}
