package com.glm.glmback.pupitre.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class AmplitudeMaximaleTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new AmplitudeMaximale(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("amplitude maximale");
  }
}
