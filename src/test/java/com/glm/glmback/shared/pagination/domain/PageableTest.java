package com.glm.glmback.shared.pagination.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.NumberValueTooHighException;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import org.junit.jupiter.api.Test;

@UnitTest
class PageableTest {

  @Test
  void shouldNotBuildWithNegativePage() {
    assertThatThrownBy(() -> new Pageable(-1, 10))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("page");
  }

  @Test
  void shouldNotBuildWithoutSize() {
    assertThatThrownBy(() -> new Pageable(0, 0))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("size");
  }

  @Test
  void shouldNotBuildWithSizeOverMaximum() {
    assertThatThrownBy(() -> new Pageable(0, 101))
      .isExactlyInstanceOf(NumberValueTooHighException.class)
      .hasMessageContaining("size");
  }

  @Test
  void shouldBuildPageable() {
    Pageable pageable = new Pageable(2, 20);

    assertThat(pageable.page()).isEqualTo(2);
    assertThat(pageable.size()).isEqualTo(20);
  }

  @Test
  void shouldGetOffsetFromPageAndSize() {
    assertThat(new Pageable(3, 20).offset()).isEqualTo(60);
  }
}
