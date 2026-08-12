package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class OperateurTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new Operateur(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new Operateur(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    String tooLong = "a".repeat(100 + 1);

    assertThatThrownBy(() -> new Operateur(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldGetValueFromValidOperateur() {
    assertThat(OPERATEUR_DUPONT.value()).isEqualTo("dupont");
  }
}
