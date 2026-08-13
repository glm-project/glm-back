package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
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
    String tooLong = "a".repeat(51);

    assertThatThrownBy(() -> new Matricule(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("matricule");
  }

  @Test
  void shouldGetValueFromValidMatricule() {
    assertThat(MATRICULE_049.value()).isEqualTo("049");
  }

  @Test
  void shouldNotBuildOptionalMatriculeFromNull() {
    assertThat(Matricule.of(null)).isEmpty();
  }

  @Test
  void shouldNotBuildOptionalMatriculeFromBlank() {
    assertThat(Matricule.of(" ")).isEmpty();
  }

  @Test
  void shouldBuildOptionalMatriculeFromValue() {
    assertThat(Matricule.of("049")).contains(MATRICULE_049);
  }
}
