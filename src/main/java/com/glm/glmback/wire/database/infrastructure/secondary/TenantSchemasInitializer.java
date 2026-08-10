package com.glm.glmback.wire.database.infrastructure.secondary;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Component;

@Component
class TenantSchemasInitializer implements InitializingBean {

  private final DataSource dataSource;
  private final TenantSchemas tenantSchemas;
  private final String changeLog;

  TenantSchemasInitializer(DataSource dataSource, TenantSchemas tenantSchemas, @Value("${spring.liquibase.change-log}") String changeLog) {
    this.dataSource = dataSource;
    this.tenantSchemas = tenantSchemas;
    this.changeLog = changeLog;
  }

  @Override
  public void afterPropertiesSet() throws LiquibaseException, SQLException {
    for (String schema : tenantSchemas.schemas()) {
      createSchema(schema);
      migrate(schema);
    }
  }

  /**
   * Le pool est configure en {@code auto-commit: false} : sans validation explicite, le schema serait
   * annule au retour de la connexion et Liquibase ne trouverait rien ou migrer.
   */
  private void createSchema(String schema) throws SQLException {
    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      connection.setAutoCommit(true);
      statement.execute("CREATE SCHEMA IF NOT EXISTS \"%s\"".formatted(schema));
    }
  }

  private void migrate(String schema) throws LiquibaseException {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setResourceLoader(new DefaultResourceLoader());
    liquibase.setChangeLog(changeLog);
    liquibase.setDefaultSchema(schema);
    liquibase.setLiquibaseSchema(schema);
    liquibase.afterPropertiesSet();
  }
}
