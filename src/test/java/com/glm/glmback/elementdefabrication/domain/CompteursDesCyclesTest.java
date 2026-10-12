package com.glm.glmback.elementdefabrication.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import org.junit.jupiter.api.Test;

@UnitTest
class CompteursDesCyclesTest {

  @Test
  void shouldNotCompterNegativementEnPreparation() {
    assertThatThrownBy(() -> new CompteursDesCycles(-1, 0, 0))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("enPreparation");
  }

  @Test
  void shouldNotCompterNegativementEnAtelier() {
    assertThatThrownBy(() -> new CompteursDesCycles(0, -1, 0))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("enAtelier");
  }

  @Test
  void shouldNotCompterNegativementLesClotures() {
    assertThatThrownBy(() -> new CompteursDesCycles(0, 0, -1))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("clotures");
  }
}
