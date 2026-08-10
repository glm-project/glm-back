package com.glm.glmback.wire.database.infrastructure.secondary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application.multitenancy")
class MultitenancyProperties {

  private final List<TenantProperties> tenants = new ArrayList<>();
  private String defaultSchema = "public";

  public String getDefaultSchema() {
    return defaultSchema;
  }

  public void setDefaultSchema(String defaultSchema) {
    this.defaultSchema = defaultSchema;
  }

  public List<TenantProperties> getTenants() {
    return Collections.unmodifiableList(tenants);
  }

  public void setTenants(List<TenantProperties> tenants) {
    this.tenants.addAll(tenants);
  }

  public static class TenantProperties {

    private String id;
    private String schema;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getSchema() {
      return schema;
    }

    public void setSchema(String schema) {
      this.schema = schema;
    }
  }
}
