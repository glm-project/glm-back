package com.glm.glmback.syntheseheures.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class DescriptionDElementTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new DescriptionDElement(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("description");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    assertThatThrownBy(() -> new DescriptionDElement("X".repeat(1001)))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("description");
  }

  @Test
  void shouldBuildWithValue() {
    assertThat(new DescriptionDElement("Carter de pompe").value()).isEqualTo("Carter de pompe");
  }
}
