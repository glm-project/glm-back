package com.glm.glmback.coutderevient.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class LibelleDePosteTest {

  @Test
  void shouldNotBuildBlank() {
    assertThatThrownBy(() -> new LibelleDePoste(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle du poste");
  }

  @Test
  void shouldNotBuildLongerThanItsColumn() {
    assertThatThrownBy(() -> new LibelleDePoste("a".repeat(101)))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("libelle du poste");
  }

  @Test
  void shouldKeepItsValue() {
    assertThat(new LibelleDePoste("a".repeat(100)).value()).hasSize(100);
  }
}
