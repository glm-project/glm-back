package com.glm.glmback.wire.database.infrastructure.secondary;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application.multitenancy")
class MultitenancyProperties {

  private String defaultSchema = "public";
  private String seedChangeLog;

  public String getDefaultSchema() {
    return defaultSchema;
  }

  public void setDefaultSchema(String defaultSchema) {
    this.defaultSchema = defaultSchema;
  }

  public String getSeedChangeLog() {
    return seedChangeLog;
  }

  public void setSeedChangeLog(String seedChangeLog) {
    this.seedChangeLog = seedChangeLog;
  }
}
