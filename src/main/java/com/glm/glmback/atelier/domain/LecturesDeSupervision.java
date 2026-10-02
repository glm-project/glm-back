package com.glm.glmback.atelier.domain;

import java.time.Instant;

public interface LecturesDeSupervision {
  LectureDeSupervision read(Instant evaluation);
}
