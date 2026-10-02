package com.glm.glmback.coutderevient.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class NomDOperateurTest {

  @Test
  void shouldNotBuildBlank() {
    assertThatThrownBy(() -> new NomDOperateur(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("nom de l'operateur");
  }

  @Test
  void shouldNotBuildLongerThanItsColumn() {
    assertThatThrownBy(() -> new NomDOperateur("a".repeat(101)))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("nom de l'operateur");
  }

  @Test
  void shouldKeepItsValue() {
    assertThat(new NomDOperateur("a".repeat(100)).value()).hasSize(100);
  }
}
