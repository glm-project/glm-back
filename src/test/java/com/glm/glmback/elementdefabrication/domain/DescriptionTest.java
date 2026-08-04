package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import org.junit.jupiter.api.Test;

@UnitTest
class DescriptionTest {

  @Test
  void shouldNotBuildWithoutDescription() {
    assertThatThrownBy(() -> new Description(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("description");
  }

  @Test
  void shouldNotBuildWithBlankDescription() {
    assertThatThrownBy(() -> new Description(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("description");
  }

  @Test
  void shouldNotBuildWithTooLongDescription() {
    String tooLong = "a".repeat(1001);

    assertThatThrownBy(() -> new Description(tooLong))
      .isExactlyInstanceOf(StringTooLongException.class)
      .hasMessageContaining("description");
  }

  @Test
  void shouldGetValueFromValidDescription() {
    assertThat(descriptionCarterEnFonte().value()).isEqualTo("Carter en fonte");
  }
}
