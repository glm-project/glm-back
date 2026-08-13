package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;

public interface PosteDeTravailRepository {
  PosteDeTravail create(PosteDeTravail poste);

  PosteDeTravail update(PosteDeTravail poste);

  void delete(PosteDeTravailId id);

  Optional<PosteDeTravail> get(PosteDeTravailId id);

  Optional<PosteDeTravailId> idPourLibelle(Libelle libelle);

  Page<PosteDeTravail> list(PosteDeTravailCriteria criteria, Pageable pageable);
}
