package com.glm.glmback.parametrage.application;

import static com.glm.glmback.parametrage.domain.ParametrageFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ParametrageLuTest {

  @Test
  void shouldNotBuildWithoutParametrage() {
    assertThatThrownBy(() -> new ParametrageLu(null, Optional.empty()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("parametrage");
  }

  @Test
  void shouldNotBuildWithoutVersionDuLogo() {
    assertThatThrownBy(() -> new ParametrageLu(parametrageDixHeures(), null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("versionDuLogo");
  }
}
