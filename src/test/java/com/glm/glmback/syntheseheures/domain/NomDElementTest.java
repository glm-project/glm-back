package com.glm.glmback.syntheseheures.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class NomDElementTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new NomDElement(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom de l'element");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    assertThatThrownBy(() -> new NomDElement("X".repeat(31)))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("nom de l'element");
  }

  @Test
  void shouldBuildWithValue() {
    assertThat(new NomDElement("PRD-2026-000015").value()).isEqualTo("PRD-2026-000015");
  }
}
