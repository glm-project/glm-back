package com.glm.glmback.coutderevient.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import org.junit.jupiter.api.Test;

@UnitTest
class DiviseurTest {

  @Test
  void shouldNotBuildWithoutPoste() {
    assertThatThrownBy(() -> new Diviseur(0))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("diviseur");
  }

  @Test
  void shouldBuildWithPostes() {
    assertThat(new Diviseur(2).value()).isEqualTo(2);
  }
}
