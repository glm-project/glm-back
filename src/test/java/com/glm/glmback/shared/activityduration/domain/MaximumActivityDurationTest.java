package com.glm.glmback.shared.activityduration.domain;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
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
   * A zero or negative duration would make every activity due as soon as it begins.
   */
  @Test
  void shouldNotBuildWithAValueThatIsNotStrictlyPositive() {
    assertThatThrownBy(() -> new MaximumActivityDuration(Duration.ZERO))
      .isExactlyInstanceOf(NumberValueTooLowException.class)
      .hasMessageContaining("maximum activity duration");
    assertThatThrownBy(() -> new MaximumActivityDuration(Duration.ofNanos(-1))).isExactlyInstanceOf(NumberValueTooLowException.class);
    assertThat(new MaximumActivityDuration(Duration.ofNanos(1)).value()).isEqualTo(Duration.ofNanos(1));
  }
}
