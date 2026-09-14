package com.glm.glmback.pupitre.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class ReferenceDElementTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new ReferenceDElement(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("reference de l'element");
  }

  @Test
  void shouldNotBuildWithBlankValue() {
    assertThatThrownBy(() -> new ReferenceDElement(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("reference de l'element");
  }

  @Test
  void shouldNotBuildWithTooLongValue() {
    String tropLong = "a".repeat(100 + 1);

    assertThatThrownBy(() -> new ReferenceDElement(tropLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("reference de l'element");
  }

  @Test
  void shouldBuildWithValue() {
    assertThat(new ReferenceDElement("M-1187").value()).isEqualTo("M-1187");
  }

  @Test
  void shouldNAvoirAucuneValeurSansSaisie() {
    assertThat(ReferenceDElement.of(null)).isEmpty();
  }

  @Test
  void shouldNAvoirAucuneValeurPourUneSaisieVide() {
    assertThat(ReferenceDElement.of(" ")).isEmpty();
  }

  @Test
  void shouldPorterLaValeurSaisie() {
    assertThat(ReferenceDElement.of("M-1187")).contains(new ReferenceDElement("M-1187"));
  }
}
