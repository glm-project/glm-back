package com.glm.glmback.pupitre.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class LibelleDePosteTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new LibelleDePoste(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle du poste");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new LibelleDePoste(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("libelle du poste");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    String tropLong = "a".repeat(100 + 1);

    assertThatThrownBy(() -> new LibelleDePoste(tropLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("libelle du poste");
  }

  @Test
  void shouldBuildWithValue() {
    assertThat(new LibelleDePoste("Fraiseuse 1").value()).isEqualTo("Fraiseuse 1");
  }
}
