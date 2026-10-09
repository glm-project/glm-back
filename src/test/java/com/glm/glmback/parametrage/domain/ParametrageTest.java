package com.glm.glmback.parametrage.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ParametrageTest {

  @Test
  void shouldNotBuildWithoutDureeMaxDActivite() {
    assertThatThrownBy(() -> new Parametrage(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("dureeMaxDActivite");
  }
}
