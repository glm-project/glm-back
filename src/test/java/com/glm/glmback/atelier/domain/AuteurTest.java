package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class AuteurTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new Auteur(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("auteur");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new Auteur(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("auteur");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    String tooLong = "a".repeat(100 + 1);

    assertThatThrownBy(() -> new Auteur(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("auteur");
  }

  @Test
  void shouldGetValueFromValidAuteur() {
    assertThat(AUTEUR_LEROY.value()).isEqualTo("leroy");
  }
}
