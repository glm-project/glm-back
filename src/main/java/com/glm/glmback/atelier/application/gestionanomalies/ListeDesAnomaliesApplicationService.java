package com.glm.glmback.atelier.application.gestionanomalies;

import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.gestionanomalies.AnomaliesDAtelierCriteria;
import com.glm.glmback.atelier.domain.gestionanomalies.FinsAutomatiquesDAtelier;
import com.glm.glmback.atelier.domain.gestionanomalies.LectureDesFinsAutomatiques;
import com.glm.glmback.atelier.domain.gestionanomalies.ListeDesAnomaliesService;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListeDesAnomaliesApplicationService {

  private final ListeDesAnomaliesService anomalies;
  private final Clock clock;

  public ListeDesAnomaliesApplicationService(
    FinsAutomatiquesDAtelier finsAutomatiques,
    OperateursConnus operateurs,
    PostesConnus postes,
    Clock clock
  ) {
    this.anomalies = new ListeDesAnomaliesService(finsAutomatiques, operateurs, postes);
    this.clock = clock;
  }

  /** L'instant d'evaluation est celui de l'horloge, lu une seule fois pour toute la page. */
  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public LectureDesFinsAutomatiques listFinsAutomatiques(AnomaliesDAtelierCriteria criteria, Pageable pageable) {
    return anomalies.listFinsAutomatiques(criteria, clock.now(), pageable);
  }
}
