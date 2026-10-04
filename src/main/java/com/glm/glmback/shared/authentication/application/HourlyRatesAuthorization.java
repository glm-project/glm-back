package com.glm.glmback.shared.authentication.application;

import com.glm.glmback.shared.authentication.domain.Role;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Hourly rates are confidential to workshop managers, including historical rates in activity responses.
 */
public final class HourlyRatesAuthorization {

  private HourlyRatesAuthorization() {}

  public static BigDecimal disclose(Optional<BigDecimal> rate) {
    return rate.filter(value -> AuthenticatedUser.roles().hasRole(Role.GESTIONNAIRE)).orElse(null);
  }
}
