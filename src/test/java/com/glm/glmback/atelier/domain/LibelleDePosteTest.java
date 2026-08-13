package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
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
    String tooLong = "a".repeat(100 + 1);

    assertThatThrownBy(() -> new LibelleDePoste(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("libelle du poste");
  }

  @Test
  void shouldGetValueFromValidLibelleDePoste() {
    assertThat(LIBELLE_FRAISEUSE_1.value()).isEqualTo("Fraiseuse 1");
  }
}
