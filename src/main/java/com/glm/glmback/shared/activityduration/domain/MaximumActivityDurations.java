package com.glm.glmback.shared.activityduration.domain;

/**
 * The port through which a context reads the maximum activity duration currently in force.
 *
 * <p>
 * The company sets that duration, so the setting lives in the context that owns the company's settings and is
 * implemented there. The workshop and the pupitre receive it through this port, never as a constant: neither of them
 * knows the default, nor the bounds, nor where the value is kept.
 * </p>
 */
public interface MaximumActivityDurations {
  /**
   * The duration in force at the moment of the call. It is read on every call, never cached: the manager may change it
   * between two calls.
   */
  MaximumActivityDuration current();
}
