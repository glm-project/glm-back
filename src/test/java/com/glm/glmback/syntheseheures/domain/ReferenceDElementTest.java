package com.glm.glmback.syntheseheures.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class ReferenceDElementTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new ReferenceDElement(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("reference");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    assertThatThrownBy(() -> new ReferenceDElement("X".repeat(101)))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("reference");
  }

  @Test
  void shouldBuildWithValue() {
    assertThat(new ReferenceDElement("1015").value()).isEqualTo("1015");
  }
}
