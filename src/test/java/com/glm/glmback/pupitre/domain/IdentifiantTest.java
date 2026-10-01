package com.glm.glmback.pupitre.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class IdentifiantTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new Identifiant(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("identifiant");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new Identifiant(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("identifiant");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    String tropLong = "a".repeat(50 + 1);

    assertThatThrownBy(() -> new Identifiant(tropLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("identifiant");
  }

  @Test
  void shouldBuildWithValue() {
    assertThat(new Identifiant("049").value()).isEqualTo("049");
  }

  @Test
  void shouldNAvoirAucuneValeurSansSaisie() {
    assertThat(Identifiant.of(null)).isEmpty();
  }

  @Test
  void shouldNAvoirAucuneValeurPourUneSaisieVide() {
    assertThat(Identifiant.of(" ")).isEmpty();
  }

  @Test
  void shouldPorterLaValeurSaisie() {
    assertThat(Identifiant.of("049")).contains(new Identifiant("049"));
  }
}
