package com.glm.glmback.feuilledetemps.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class NomTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new Nom(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new Nom(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    assertThatThrownBy(() -> new Nom("D".repeat(101)))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldBuildWithValue() {
    assertThat(new Nom("Dupont").value()).isEqualTo("Dupont");
  }
}
