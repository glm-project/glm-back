package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.LectureDeSupervision;
import com.glm.glmback.atelier.domain.LecturesDeSupervision;
import com.glm.glmback.shared.time.domain.Clock;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupervisionDAtelierApplicationService {

  private final Clock clock;
  private final LecturesDeSupervision lectures;

  public SupervisionDAtelierApplicationService(Clock clock, LecturesDeSupervision lectures) {
    this.clock = clock;
    this.lectures = lectures;
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  @Transactional(readOnly = true)
  public LectureDeSupervision read() {
    return lectures.read(clock.now());
  }
}
