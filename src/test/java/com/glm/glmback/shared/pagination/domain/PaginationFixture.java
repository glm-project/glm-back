package com.glm.glmback.shared.pagination.domain;

import java.util.List;

public final class PaginationFixture {

  private PaginationFixture() {}

  public static Pageable firstPageOfTen() {
    return new Pageable(0, 10);
  }

  public static List<String> firstAndSecond() {
    return List.of("first", "second");
  }

  public static Page<String> pageWithoutContent(int currentPage, int pageSize, long totalElementsCount) {
    return Page.<String>builder().content(List.of()).currentPage(currentPage).pageSize(pageSize).totalElementsCount(totalElementsCount);
  }
}
