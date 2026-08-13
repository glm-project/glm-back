package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class NatureDeTravailTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new NatureDeTravail(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de travail");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new NatureDeTravail(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nature de travail");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    String tooLong = "a".repeat(51);

    assertThatThrownBy(() -> new NatureDeTravail(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("nature de travail");
  }

  @Test
  void shouldGetValueFromValidNature() {
    assertThat(NATURE_TOURNAGE.value()).isEqualTo("tournage");
  }

  @Test
  void shouldOrderNaturesAlphabetically() {
    assertThat(NATURE_SOUDAGE).isLessThan(NATURE_TOURNAGE);
    assertThat(NATURE_TOURNAGE).isGreaterThan(NATURE_SOUDAGE);
  }
}
