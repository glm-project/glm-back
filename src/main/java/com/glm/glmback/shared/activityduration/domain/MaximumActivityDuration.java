package com.glm.glmback.shared.activityduration.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;

/**
 * How long an activity may run before it ends by itself: the single source of that duration.
 *
 * <p>
 * The workshop reads it to compute the due time of an activity and to judge a received punch, and the pupitre reads it
 * to know that due time offline. Both contexts depend on this kernel, never on each other. It is the workshop's rule
 * today, not a company setting: a table of settings will replace {@link #standard()} later.
 * </p>
 */
public record MaximumActivityDuration(Duration value) {
  private static final Duration STANDARD = Duration.ofHours(13);

  public MaximumActivityDuration {
    Assert.notNull("maximum activity duration", value);
  }

  public static MaximumActivityDuration standard() {
    return new MaximumActivityDuration(STANDARD);
  }
}
