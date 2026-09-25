package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Optional;

/**
 * La liste des anomalies de presence, jugees a l'instant de lecture avec le seuil courant, la plus recente d'abord.
 */
public final class AnomaliesService {

  private final JourneeDeTravailRepository journees;
  private final SeuilDAmplitude seuil;
  private final Clock clock;

  public AnomaliesService(JourneeDeTravailRepository journees, SeuilDAmplitude seuil, Clock clock) {
    this.journees = journees;
    this.seuil = seuil;
    this.clock = clock;
  }

  public Page<AnomalieDePresence> list(Optional<OperateurId> operateur, Optional<TypeDAnomalie> type, Pageable pageable) {
    CriteresDAnomalie criteres = new CriteresDAnomalie(clock.now(), seuil.amplitudeMaximale(), operateur, type);
    Page<JourneeDeTravail> page = journees.enAnomalie(criteres, pageable);

    return Page.<AnomalieDePresence>builder()
      .content(
        page
          .content()
          .stream()
          .map(journee -> AnomalieDePresence.de(journee, criteres.maintenant(), criteres.seuil()).orElseThrow())
          .toList()
      )
      .currentPage(page.currentPage())
      .pageSize(page.pageSize())
      .totalElementsCount(page.totalElementsCount());
  }
}
