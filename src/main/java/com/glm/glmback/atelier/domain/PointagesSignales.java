package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;

public interface PointagesSignales {
  PointageSignale create(PointageSignale pointage);

  PointageSignale update(PointageSignale pointage);

  Optional<PointageSignale> get(PointageSignaleId id);

  Page<PointageSignale> list(CriteresDePointageSignale criteres, Pageable pageable);
}
