package com.glm.glmback.wire.database.infrastructure.secondary;

import com.glm.glmback.shared.multitenancy.application.NotTenantedUserException;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/**
 * Un pool Hikari par entreprise, pour qu'une entreprise ne puisse pas epuiser les connexions des autres.
 * Chaque pool reprend la configuration du pool principal ; il ne s'ouvre qu'a sa premiere connexion, une
 * entreprise sans activite ne tient donc aucune connexion.
 */
@Component
class TenantDataSources implements DisposableBean {

  private final Map<Tenant, HikariDataSource> pools;

  TenantDataSources(HikariDataSource mainDataSource, TenantRegistry registry) {
    pools = registry
      .tenants()
      .stream()
      .collect(Collectors.toUnmodifiableMap(Function.identity(), tenant -> open(mainDataSource, tenant, registry.schema(tenant))));
  }

  private static HikariDataSource open(HikariDataSource mainDataSource, Tenant tenant, String schema) {
    HikariConfig config = new HikariConfig();
    mainDataSource.copyStateTo(config);
    config.setPoolName(mainDataSource.getPoolName() + "-" + tenant.value());
    config.setSchema(schema);

    HikariDataSource pool = new HikariDataSource();
    config.copyStateTo(pool);
    return pool;
  }

  HikariDataSource dataSource(Tenant tenant) {
    return Optional.ofNullable(pools.get(tenant)).orElseThrow(NotTenantedUserException::new);
  }

  @Override
  public void destroy() {
    pools.values().forEach(HikariDataSource::close);
  }
}
