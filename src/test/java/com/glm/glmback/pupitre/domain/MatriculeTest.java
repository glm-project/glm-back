package com.glm.glmback.pupitre.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class MatriculeTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new Matricule(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("matricule");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new Matricule(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("matricule");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    String tropLong = "a".repeat(50 + 1);

    assertThatThrownBy(() -> new Matricule(tropLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("matricule");
  }

  @Test
  void shouldBuildWithValue() {
    assertThat(new Matricule("049").value()).isEqualTo("049");
  }

  @Test
  void shouldNAvoirAucuneValeurSansSaisie() {
    assertThat(Matricule.of(null)).isEmpty();
  }

  @Test
  void shouldNAvoirAucuneValeurPourUneSaisieVide() {
    assertThat(Matricule.of(" ")).isEmpty();
  }

  @Test
  void shouldPorterLaValeurSaisie() {
    assertThat(Matricule.of("049")).contains(new Matricule("049"));
  }
}
