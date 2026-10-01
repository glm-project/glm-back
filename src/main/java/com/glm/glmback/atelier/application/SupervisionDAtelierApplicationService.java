package com.glm.glmback.atelier.application;

import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

@Service
public class SupervisionDAtelierApplicationService {

  private final Clock clock;

  public SupervisionDAtelierApplicationService(Clock clock) {
    this.clock = clock;
  }

  @Secured({ "ROLE_USER", "ROLE_GESTIONNAIRE" })
  public Instant evaluation() {
    return clock.now();
  }
}
