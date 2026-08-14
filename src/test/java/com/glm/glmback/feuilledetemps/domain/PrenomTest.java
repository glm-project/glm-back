package com.glm.glmback.feuilledetemps.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class PrenomTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new Prenom(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("prenom");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new Prenom(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("prenom");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    assertThatThrownBy(() -> new Prenom("J".repeat(101)))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("prenom");
  }

  @Test
  void shouldBuildWithValue() {
    assertThat(new Prenom("Jean").value()).isEqualTo("Jean");
  }
}
