package com.glm.glmback.wire.database.infrastructure.secondary;

import com.glm.glmback.shared.error.domain.Assert;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Le registre des entreprises vit dans le schema par defaut, qui ne porte aucune table metier. Le jeu
 * d'entreprises d'un environnement de developpement ou de test s'y ajoute par un changelog distinct,
 * declare par son seul profil : l'artefact livre n'en declare aucun.
 */
@Component
class AdminSchemaInitializer implements InitializingBean {

  private static final String CHANGE_LOG = "classpath:config/liquibase/admin/master.xml";

  private final DataSource dataSource;
  private final String schema;
  private final String seedChangeLog;

  AdminSchemaInitializer(DataSource dataSource, MultitenancyProperties properties) {
    Assert.field("default-schema", properties.getDefaultSchema()).notBlank().matches(SchemaNames.PATTERN);

    this.dataSource = dataSource;
    this.schema = properties.getDefaultSchema();
    this.seedChangeLog = properties.getSeedChangeLog();
  }

  @Override
  public void afterPropertiesSet() throws LiquibaseException {
    LiquibaseMigration.migrate(dataSource, CHANGE_LOG, schema);
    if (StringUtils.isNotBlank(seedChangeLog)) {
      LiquibaseMigration.migrate(dataSource, seedChangeLog, schema);
    }
  }

  String schema() {
    return schema;
  }
}
