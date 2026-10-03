package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record RevisionDuSuivi(long value) {
  public RevisionDuSuivi {
    Assert.field("revision", value).min(0);
  }
}
