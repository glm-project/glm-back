package com.glm.glmback.syntheseheures.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class LibelleDePosteTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new LibelleDePoste(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle du poste");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    assertThatThrownBy(() -> new LibelleDePoste("X".repeat(101)))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("libelle du poste");
  }

  @Test
  void shouldBuildWithValue() {
    assertThat(new LibelleDePoste("DMU 50").value()).isEqualTo("DMU 50");
  }
}
