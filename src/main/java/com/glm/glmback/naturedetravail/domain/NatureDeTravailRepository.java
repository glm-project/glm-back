package com.glm.glmback.naturedetravail.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;

public interface NatureDeTravailRepository {
  NatureDeTravail create(NatureDeTravail nature);

  NatureDeTravail update(NatureDeTravail nature);

  void delete(NatureDeTravailId id);

  Optional<NatureDeTravail> get(NatureDeTravailId id);

  Optional<NatureDeTravailId> idPourCle(CleDeNature cle);

  /**
   * Par cle, donc dans l'ordre alphabetique sans egard aux accents ni a la casse, puis par identifiant.
   */
  Page<NatureDeTravail> list(Pageable pageable);
}
