package com.glm.glmback.parametrage.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NumberValueTooHighException;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@UnitTest
class DureeMaxDActiviteTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new DureeMaxDActivite(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("dureeMaxDActivite");
  }

  @Test
  void shouldNotBuildBelowOneHour() {
    assertThatThrownBy(() -> new DureeMaxDActivite(Duration.ofMinutes(59)))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("dureeMaxDActivite");
  }

  @Test
  void shouldNotBuildAboveOneDay() {
    assertThatThrownBy(() -> new DureeMaxDActivite(Duration.ofHours(24).plusSeconds(1)))
      .isExactlyInstanceOf(NumberValueTooHighException.class)
      .hasMessageContaining("dureeMaxDActivite");
  }

  @Test
  void shouldBuildOnBounds() {
    assertThat(new DureeMaxDActivite(Duration.ofHours(1)).value()).isEqualTo(Duration.ofHours(1));
    assertThat(new DureeMaxDActivite(Duration.ofHours(24)).value()).isEqualTo(Duration.ofHours(24));
  }

  @Test
  void shouldLastThirteenHoursByDefault() {
    assertThat(DureeMaxDActivite.parDefaut()).isEqualTo(new DureeMaxDActivite(Duration.ofHours(13)));
  }
}
