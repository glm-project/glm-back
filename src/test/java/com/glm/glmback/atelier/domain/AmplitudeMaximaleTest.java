package com.glm.glmback.atelier.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@UnitTest
class AmplitudeMaximaleTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new AmplitudeMaximale(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("amplitude maximale");
  }

  @Test
  void shouldGetValue() {
    assertThat(new AmplitudeMaximale(Duration.ofHours(13)).value()).isEqualTo(Duration.ofHours(13));
  }
}
