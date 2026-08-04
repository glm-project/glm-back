package com.glm.glmback.shared.pagination.domain;

import com.glm.glmback.shared.error.domain.Assert;

public record Pageable(int page, int size) {
  private static final int MAX_SIZE = 100;

  public Pageable {
    Assert.field("page", page).min(0);
    Assert.field("size", size).min(1).max(MAX_SIZE);
  }

  public long offset() {
    return (long) page * size;
  }
}
