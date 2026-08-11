package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class ReferenceTest {

  @Test
  void shouldNotBuildWithoutReference() {
    assertThatThrownBy(() -> new Reference(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("reference");
  }

  @Test
  void shouldNotBuildWithBlankReference() {
    assertThatThrownBy(() -> new Reference(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("reference");
  }

  @Test
  void shouldNotBuildWithTooLongReference() {
    String tooLong = "a".repeat(101);

    assertThatThrownBy(() -> new Reference(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("reference");
  }

  @Test
  void shouldGetValueFromValidReference() {
    assertThat(reference1015().value()).isEqualTo("1015");
  }

  @Test
  void shouldNotBuildOptionalReferenceFromNull() {
    assertThat(Reference.of(null)).isEmpty();
  }

  @Test
  void shouldNotBuildOptionalReferenceFromBlank() {
    assertThat(Reference.of(" ")).isEmpty();
  }

  @Test
  void shouldBuildOptionalReferenceFromValue() {
    assertThat(Reference.of("1015")).contains(reference1015());
  }
}
