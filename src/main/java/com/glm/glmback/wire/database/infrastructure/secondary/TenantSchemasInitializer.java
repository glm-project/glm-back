package com.glm.glmback.wire.database.infrastructure.secondary;

import com.glm.glmback.shared.multitenancy.domain.Tenant;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Chaque schema est cree et migre sur la base de son entreprise, qui n'est pas forcement la base principale.
 * Une base injoignable empeche donc le demarrage.
 */
@Component
class TenantSchemasInitializer implements InitializingBean {

  private final TenantRegistry tenantRegistry;
  private final TenantDataSources tenantDataSources;
  private final String changeLog;

  TenantSchemasInitializer(
    TenantRegistry tenantRegistry,
    TenantDataSources tenantDataSources,
    @Value("${spring.liquibase.change-log}") String changeLog
  ) {
    this.tenantRegistry = tenantRegistry;
    this.tenantDataSources = tenantDataSources;
    this.changeLog = changeLog;
  }

  @Override
  public void afterPropertiesSet() throws LiquibaseException, SQLException {
    for (Tenant tenant : tenantRegistry.tenants()) {
      DataSource dataSource = tenantDataSources.unpooled(tenant);
      String schema = tenantRegistry.schema(tenant);
      createSchema(dataSource, schema);
      LiquibaseMigration.migrate(dataSource, changeLog, schema);
    }
  }

  private static void createSchema(DataSource dataSource, String schema) throws SQLException {
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      connection.setAutoCommit(true);
      statement.execute("CREATE SCHEMA IF NOT EXISTS \"%s\"".formatted(schema));
    }
  }
}
