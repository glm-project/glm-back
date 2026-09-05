package com.glm.glmback.shared.pagination.infrastructure.primary;

import com.glm.glmback.shared.pagination.domain.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;

@Schema(name = "Page")
public record RestPage<T>(
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<T> content,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int currentPage,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int pageSize,
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalElementsCount
) {
  private RestPage(Builder<T> builder) {
    this(builder.content, builder.currentPage, builder.pageSize, builder.totalElementsCount);
  }

  public static <S, T> RestPage<T> from(Page<S> page, Function<S, T> mapper) {
    return RestPage.<T>builder()
      .content(page.content().stream().map(mapper).toList())
      .currentPage(page.currentPage())
      .pageSize(page.pageSize())
      .totalElementsCount(page.totalElementsCount());
  }

  private static <T> PageContentBuilder<T> builder() {
    return new Builder<>();
  }

  private interface PageContentBuilder<T> {
    PageCurrentPageBuilder<T> content(List<T> content);
  }

  private interface PageCurrentPageBuilder<T> {
    PagePageSizeBuilder<T> currentPage(int currentPage);
  }

  private interface PagePageSizeBuilder<T> {
    PageTotalElementsCountBuilder<T> pageSize(int pageSize);
  }

  private interface PageTotalElementsCountBuilder<T> {
    RestPage<T> totalElementsCount(long totalElementsCount);
  }

  private static final class Builder<
    T
  > implements PageContentBuilder<T>, PageCurrentPageBuilder<T>, PagePageSizeBuilder<T>, PageTotalElementsCountBuilder<T> {

    private List<T> content;
    private int currentPage;
    private int pageSize;
    private long totalElementsCount;

    @Override
    public PageCurrentPageBuilder<T> content(List<T> content) {
      this.content = content;
      return this;
    }

    @Override
    public PagePageSizeBuilder<T> currentPage(int currentPage) {
      this.currentPage = currentPage;
      return this;
    }

    @Override
    public PageTotalElementsCountBuilder<T> pageSize(int pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    @Override
    public RestPage<T> totalElementsCount(long totalElementsCount) {
      this.totalElementsCount = totalElementsCount;
      return new RestPage<>(this);
    }
  }
}
