package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class PrefixeTest {

  @Test
  void shouldNotBuildWithoutPrefixe() {
    assertThatThrownBy(() -> new Prefixe(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("prefixe");
  }

  @Test
  void shouldNotBuildWithBlankPrefixe() {
    assertThatThrownBy(() -> new Prefixe(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("prefixe");
  }

  @Test
  void shouldNotBuildWithTooLongPrefixe() {
    String tooLong = "a".repeat(11);

    assertThatThrownBy(() -> new Prefixe(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("prefixe");
  }

  @Test
  void shouldGetValueFromValidPrefixe() {
    assertThat(PREFIXE_OF.value()).isEqualTo("OF");
  }
}
