package com.glm.glmback.atelier.gestionconflits.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;

/** Lecture paginee des sequences projetees ; une ligne par sequence, meme sur un suivi cloture. */
public interface ConflitsDAtelier {
  Page<ConflitEnListe> list(ConflitsDAtelierCriteria criteria, Pageable pageable);
}
