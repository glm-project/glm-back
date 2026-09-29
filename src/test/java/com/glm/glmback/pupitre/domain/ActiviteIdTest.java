package com.glm.glmback.pupitre.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import org.junit.jupiter.api.Test;

@UnitTest
class ActiviteIdTest {

  @Test
  void shouldNotBuildWithoutIdentity() {
    assertThatThrownBy(() -> new ActiviteId(null)).isExactlyInstanceOf(MissingMandatoryValueException.class);
  }
}
