package com.glm.glmback.pupitre.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class NomDElementTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new NomDElement(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom de l'element");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new NomDElement(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom de l'element");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    String tropLong = "a".repeat(30 + 1);

    assertThatThrownBy(() -> new NomDElement(tropLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("nom de l'element");
  }

  @Test
  void shouldBuildWithValue() {
    assertThat(new NomDElement("OF-2026-000042").value()).isEqualTo("OF-2026-000042");
  }
}
