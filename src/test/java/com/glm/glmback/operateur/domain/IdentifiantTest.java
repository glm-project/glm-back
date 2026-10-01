package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringNotMatchingPatternException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

  @ParameterizedTest
  @ValueSource(strings = { "1234567", "12A", "AB-12", "-12", "12 3", "１２" })
  void shouldNotBuildWithAnythingButOneToSixDigits(String value) {
    assertThatThrownBy(() -> new Identifiant(value))
      .isExactlyInstanceOf(StringNotMatchingPatternException.class)
      .hasMessageContaining("identifiant");
  }

  @ParameterizedTest
  @ValueSource(strings = { "0", "7", "007", "123456" })
  void shouldBuildWithOneToSixDigits(String value) {
    assertThat(new Identifiant(value).value()).isEqualTo(value);
  }

  @Test
  void shouldKeepLeadingZeros() {
    assertThat(new Identifiant("007")).isNotEqualTo(new Identifiant("7"));
  }

  @Test
  void shouldGetValueFromValidIdentifiant() {
    assertThat(IDENTIFIANT_049.value()).isEqualTo("049");
  }

  @Test
  void shouldNotBuildOptionalIdentifiantFromNull() {
    assertThat(Identifiant.of(null)).isEmpty();
  }

  @Test
  void shouldNotBuildOptionalIdentifiantFromBlank() {
    assertThat(Identifiant.of(" ")).isEmpty();
  }

  @Test
  void shouldBuildOptionalIdentifiantFromValue() {
    assertThat(Identifiant.of("049")).contains(IDENTIFIANT_049);
  }
}
