package com.glm.glmback.wire.database.infrastructure.secondary;

import java.util.Map;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.context.spi.TenantSchemaMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * La connexion vient du pool de l'entreprise ({@link TenantConnectionProvider}), puis Hibernate y pose le
 * schema de l'entreprise ({@code TenantSchemaMapper}) et le restaure a la liberation.
 */
@Configuration
class MultitenancyHibernateConfiguration implements HibernatePropertiesCustomizer {

  private final CurrentTenantIdentifierResolver<String> currentTenantResolver;
  private final TenantConnectionProvider connectionProvider;
  private final TenantRegistry registry;

  MultitenancyHibernateConfiguration(
    CurrentTenantIdentifierResolver<String> currentTenantResolver,
    TenantConnectionProvider connectionProvider,
    TenantRegistry registry
  ) {
    this.currentTenantResolver = currentTenantResolver;
    this.connectionProvider = connectionProvider;
    this.registry = registry;
  }

  @Override
  public void customize(Map<String, Object> hibernateProperties) {
    hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantResolver);
    hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
    hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_SCHEMA_MAPPER, (TenantSchemaMapper<String>) registry::schemaName);
  }
}
