package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
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
    String tooLong = "a".repeat(51);

    assertThatThrownBy(() -> new Identifiant(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("identifiant");
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
