package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.time.Instant;
import java.util.Optional;

public interface SuiviDAtelierRepository {
  SuiviDAtelier create(SuiviDAtelier suivi);

  SuiviDAtelier update(SuiviDAtelier suivi);

  Optional<SuiviDAtelier> get(SuiviDAtelierId id);

  Optional<SuiviDAtelier> getEnCoursPour(ElementEngageId element);

  Optional<Instant> dernierPointageDe(OperateurId operateur, Periode periode);

  Page<SuiviDAtelier> list(SuiviDAtelierCriteria criteria, Pageable pageable);
}
