package com.glm.glmback.shared.activityduration.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

@UnitTest
class MaximumActivityDurationTest {

  @Test
  void shouldNotBuildWithoutValue() {
    assertThatThrownBy(() -> new MaximumActivityDuration(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("maximum activity duration");
  }

  /**
   * The single source of the rule: an activity that nothing ended stops by itself thirteen elapsed hours after it began.
   */
  @Test
  void shouldBeThirteenHoursByDefault() {
    assertThat(MaximumActivityDuration.standard().value()).isEqualTo(Duration.ofHours(13));
  }
}
