package com.glm.glmback.coutderevient.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class NatureDOperationTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new NatureDOperation(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de l'operation");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    assertThatThrownBy(() -> new NatureDOperation("F".repeat(51)))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("nature de l'operation");
  }

  @Test
  void shouldBuildWithValue() {
    assertThat(new NatureDOperation("Fraisage").value()).isEqualTo("Fraisage");
  }
}
