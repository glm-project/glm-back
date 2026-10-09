package com.glm.glmback.shared.activityduration.domain;

import com.glm.glmback.shared.error.domain.Assert;
import com.glm.glmback.shared.error.domain.NumberValueTooLowException;
import java.time.Duration;

/**
 * How long an activity may run before it ends by itself, as the company has set it.
 *
 * <p>
 * The workshop reads it to compute the due time of an activity and to judge a received punch, and the pupitre reads it
 * to know that due time offline. Both contexts receive it through the {@link MaximumActivityDurations} port, which the
 * company settings implement: they own the value, its default and its bounds, so this kernel holds no duration of its
 * own and the workshop and the pupitre never depend on each other. It is strictly positive: a zero or negative
 * duration would make every activity due as soon as it begins.
 * </p>
 */
public record MaximumActivityDuration(Duration value) {
  public MaximumActivityDuration {
    Assert.notNull("maximum activity duration", value);
    if (!value.isPositive()) {
      throw NumberValueTooLowException.builder()
        .field("maximum activity duration")
        .minValue("above " + Duration.ZERO)
        .value(value.toString())
        .build();
    }
  }
}
