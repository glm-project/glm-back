package com.glm.glmback.parametrage.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
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
  void shouldGetValue() {
    assertThat(new Auteur("leroy").value()).isEqualTo("leroy");
  }
}
