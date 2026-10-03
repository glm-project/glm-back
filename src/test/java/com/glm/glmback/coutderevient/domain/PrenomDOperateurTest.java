package com.glm.glmback.coutderevient.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class PrenomDOperateurTest {

  @Test
  void shouldNotBuildBlank() {
    assertThatThrownBy(() -> new PrenomDOperateur(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("prenom de l'operateur");
  }

  @Test
  void shouldNotBuildLongerThanItsColumn() {
    assertThatThrownBy(() -> new PrenomDOperateur("a".repeat(101)))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("prenom de l'operateur");
  }

  @Test
  void shouldKeepItsValue() {
    assertThat(new PrenomDOperateur("a".repeat(100)).value()).hasSize(100);
  }
}
