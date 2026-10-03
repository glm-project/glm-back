package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.ConflitsDAtelier;
import com.glm.glmback.atelier.domain.ConflitsDAtelierCriteria;
import com.glm.glmback.atelier.domain.LectureDesConflits;
import com.glm.glmback.atelier.domain.ListeDesConflitsService;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.shared.pagination.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListeDesConflitsApplicationService {

  private final ListeDesConflitsService conflits;

  public ListeDesConflitsApplicationService(ConflitsDAtelier conflits, OperateursConnus operateurs, PostesConnus postes) {
    this.conflits = new ListeDesConflitsService(conflits, operateurs, postes);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public LectureDesConflits list(ConflitsDAtelierCriteria criteria, Pageable pageable) {
    return conflits.list(criteria, pageable);
  }
}
