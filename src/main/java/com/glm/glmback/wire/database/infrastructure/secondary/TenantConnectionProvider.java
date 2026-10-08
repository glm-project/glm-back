package com.glm.glmback.wire.database.infrastructure.secondary;

import com.glm.glmback.shared.multitenancy.domain.Tenant;
import javax.sql.DataSource;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.stereotype.Component;

/**
 * Hibernate prend la connexion d'une entreprise dans son propre pool ; hors requete, et pour ses besoins
 * de demarrage, dans le pool principal. Le schema reste pose par Hibernate via le {@code TenantSchemaMapper}.
 */
@Component
class TenantConnectionProvider extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String> {

  private final transient DataSource mainDataSource;
  private final transient TenantDataSources tenantDataSources;

  TenantConnectionProvider(DataSource mainDataSource, TenantDataSources tenantDataSources) {
    this.mainDataSource = mainDataSource;
    this.tenantDataSources = tenantDataSources;
  }

  @Override
  protected DataSource selectAnyDataSource() {
    return mainDataSource;
  }

  @Override
  protected DataSource selectDataSource(String tenantIdentifier) {
    if (CurrentTenantResolver.OUT_OF_REQUEST.equals(tenantIdentifier)) {
      return mainDataSource;
    }

    return tenantDataSources.dataSource(new Tenant(tenantIdentifier));
  }
}
