package com.glm.glmback.shared.pagination.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;

public record Page<T>(List<T> content, int currentPage, int pageSize, long totalElementsCount) {
  public Page {
    Assert.notNull("content", content);
    Assert.field("currentPage", currentPage).min(0);
    Assert.field("pageSize", pageSize).min(1);
    Assert.field("totalElementsCount", totalElementsCount).min(0);

    content = List.copyOf(content);
  }

  public static <T> PageContentBuilder<T> builder() {
    return content -> currentPage -> pageSize -> totalElementsCount -> new Page<>(content, currentPage, pageSize, totalElementsCount);
  }

  public int totalPagesCount() {
    return (int) Math.ceil((double) totalElementsCount / pageSize);
  }

  public boolean hasPrevious() {
    return currentPage > 0;
  }

  public boolean hasNext() {
    return currentPage + 1 < totalPagesCount();
  }

  public interface PageContentBuilder<T> {
    PageCurrentPageBuilder<T> content(List<T> content);
  }

  public interface PageCurrentPageBuilder<T> {
    PagePageSizeBuilder<T> currentPage(int currentPage);
  }

  public interface PagePageSizeBuilder<T> {
    PageTotalElementsCountBuilder<T> pageSize(int pageSize);
  }

  public interface PageTotalElementsCountBuilder<T> {
    Page<T> totalElementsCount(long totalElementsCount);
  }
}
