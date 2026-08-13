package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
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
    String tooLong = "a".repeat(100 + 1);

    assertThatThrownBy(() -> new Nom(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("nom");
  }

  @Test
  void shouldGetValueFromValidNom() {
    assertThat(NOM_DUPONT.value()).isEqualTo("Dupont");
  }
}
