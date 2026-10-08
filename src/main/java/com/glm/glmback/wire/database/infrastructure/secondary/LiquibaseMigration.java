package com.glm.glmback.wire.database.infrastructure.secondary;

import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.core.io.DefaultResourceLoader;

final class LiquibaseMigration {

  private LiquibaseMigration() {}

  /**
   * Chaque schema porte son propre {@code databasechangelog} : un schema en migration ne voit jamais
   * l'historique d'un autre.
   */
  static void migrate(DataSource dataSource, String changeLog, String schema) throws LiquibaseException {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setResourceLoader(new DefaultResourceLoader());
    liquibase.setChangeLog(changeLog);
    liquibase.setDefaultSchema(schema);
    liquibase.setLiquibaseSchema(schema);
    liquibase.afterPropertiesSet();
  }
}
