package com.glm.glmback.shared.multitenancy.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public record Tenant(String value) {
  private static final Pattern PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,62}$");

  public Tenant {
    Assert.field("tenant", value).notBlank().matches(PATTERN);
  }

  public static Optional<Tenant> of(String value) {
    return Optional.ofNullable(value).filter(StringUtils::isNotBlank).map(Tenant::new);
  }
}
