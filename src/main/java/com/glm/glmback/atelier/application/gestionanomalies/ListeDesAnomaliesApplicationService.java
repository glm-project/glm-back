package com.glm.glmback.atelier.application.gestionanomalies;

import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.gestionanomalies.AnomaliesDAtelierCriteria;
import com.glm.glmback.atelier.domain.gestionanomalies.ConflitsDAtelier;
import com.glm.glmback.atelier.domain.gestionanomalies.LectureDesConflits;
import com.glm.glmback.atelier.domain.gestionanomalies.ListeDesAnomaliesService;
import com.glm.glmback.shared.pagination.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListeDesAnomaliesApplicationService {

  private final ListeDesAnomaliesService conflits;

  public ListeDesAnomaliesApplicationService(ConflitsDAtelier conflits, OperateursConnus operateurs, PostesConnus postes) {
    this.conflits = new ListeDesAnomaliesService(conflits, operateurs, postes);
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public LectureDesConflits list(AnomaliesDAtelierCriteria criteria, Pageable pageable) {
    return conflits.list(criteria, pageable);
  }
}
