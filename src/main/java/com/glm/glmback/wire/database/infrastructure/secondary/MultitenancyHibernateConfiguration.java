package com.glm.glmback.wire.database.infrastructure.secondary;

import java.util.Map;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.context.spi.TenantSchemaMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
class MultitenancyHibernateConfiguration implements HibernatePropertiesCustomizer {

  private final CurrentTenantIdentifierResolver<String> currentTenantResolver;

  MultitenancyHibernateConfiguration(CurrentTenantIdentifierResolver<String> currentTenantResolver) {
    this.currentTenantResolver = currentTenantResolver;
  }

  @Override
  public void customize(Map<String, Object> hibernateProperties) {
    hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantResolver);
    hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_SCHEMA_MAPPER, (TenantSchemaMapper<String>) schema -> schema);
  }
}
