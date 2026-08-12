package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class NatureDOperationTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new NatureDOperation(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de l'operation");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new NatureDOperation(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de l'operation");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    String tooLong = "a".repeat(51);

    assertThatThrownBy(() -> new NatureDOperation(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("nature de l'operation");
  }

  @Test
  void shouldGetValueFromValidNatureDOperation() {
    assertThat(NATURE_FRAISAGE.value()).isEqualTo("fraisage");
  }
}
