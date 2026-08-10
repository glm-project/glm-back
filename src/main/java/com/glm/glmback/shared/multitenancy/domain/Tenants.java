package com.glm.glmback.shared.multitenancy.domain;

public interface Tenants {
  boolean contains(Tenant tenant);
}
