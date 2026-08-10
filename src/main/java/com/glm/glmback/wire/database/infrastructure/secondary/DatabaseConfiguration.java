package com.glm.glmback.wire.database.infrastructure.secondary;

import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = { "com.glm.glmback" }, enableDefaultTransactions = false)
class DatabaseConfiguration {

  @Bean
  static EntityManagerFactoryDependsOnPostProcessor tenantSchemasDependsOnPostProcessor() {
    return new EntityManagerFactoryDependsOnPostProcessor(TenantSchemasInitializer.class);
  }
}
