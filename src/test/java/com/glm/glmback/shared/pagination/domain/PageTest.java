package com.glm.glmback.shared.pagination.domain;

import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class PageTest {

  @Test
  void shouldNotBuildWithoutContent() {
    assertThatThrownBy(() -> new Page<>(null, 0, 10, 0))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("content");
  }

  @Test
  void shouldNotBuildWithNegativeCurrentPage() {
    List<String> content = List.of();

    assertThatThrownBy(() -> new Page<>(content, -1, 10, 0))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("currentPage");
  }

  @Test
  void shouldNotBuildWithoutPageSize() {
    List<String> content = List.of();

    assertThatThrownBy(() -> new Page<>(content, 0, 0, 0))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("pageSize");
  }

  @Test
  void shouldNotBuildWithNegativeTotalElementsCount() {
    List<String> content = List.of();

    assertThatThrownBy(() -> new Page<>(content, 0, 10, -1))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("totalElementsCount");
  }

  @Test
  void shouldBuildPageFromStepBuilder() {
    Page<String> page = Page.<String>builder().content(firstAndSecond()).currentPage(1).pageSize(2).totalElementsCount(5);

    assertThat(page.content()).containsExactly("first", "second");
    assertThat(page.currentPage()).isEqualTo(1);
    assertThat(page.pageSize()).isEqualTo(2);
    assertThat(page.totalElementsCount()).isEqualTo(5);
  }

  @Test
  void shouldNotBeAffectedByLaterChangesOnSourceContent() {
    List<String> source = new ArrayList<>(List.of("first"));

    Page<String> page = Page.<String>builder().content(source).currentPage(0).pageSize(10).totalElementsCount(1);
    source.add("second");

    assertThat(page.content()).containsExactly("first").isUnmodifiable();
  }

  @Test
  void shouldCountTotalPagesFromExactMultiple() {
    assertThat(pageWithoutContent(0, 10, 30).totalPagesCount()).isEqualTo(3);
  }

  @Test
  void shouldCountTotalPagesFromRemainder() {
    assertThat(pageWithoutContent(0, 10, 31).totalPagesCount()).isEqualTo(4);
  }

  @Test
  void shouldNotHavePreviousPageOnFirstPage() {
    assertThat(pageWithoutContent(0, 10, 30).hasPrevious()).isFalse();
  }

  @Test
  void shouldHavePreviousPageAfterFirstPage() {
    assertThat(pageWithoutContent(1, 10, 30).hasPrevious()).isTrue();
  }

  @Test
  void shouldHaveNextPageBeforeLastPage() {
    assertThat(pageWithoutContent(0, 10, 30).hasNext()).isTrue();
  }

  @Test
  void shouldNotHaveNextPageOnLastPage() {
    assertThat(pageWithoutContent(2, 10, 30).hasNext()).isFalse();
  }
}
