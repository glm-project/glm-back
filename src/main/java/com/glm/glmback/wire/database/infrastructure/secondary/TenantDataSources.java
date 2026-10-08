package com.glm.glmback.wire.database.infrastructure.secondary;

import com.glm.glmback.shared.multitenancy.application.NotTenantedUserException;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import com.glm.glmback.wire.database.infrastructure.secondary.TenantDeclaration.DatabaseAccess;
import com.glm.glmback.wire.database.infrastructure.secondary.TenantDeclaration.PoolSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

/**
 * Un pool Hikari par entreprise, pour qu'une entreprise ne puisse pas epuiser les connexions des autres.
 * Chaque pool reprend la configuration du pool principal, puis ce que la ligne du registre en change : base,
 * utilisateur, mot de passe (lu dans la variable d'environnement que nomme {@code secret_ref}) et taille. Il
 * ne s'ouvre qu'a sa premiere connexion : une entreprise sans activite ne tient donc aucune connexion.
 */
@Component
class TenantDataSources implements DisposableBean {

  private final Map<Tenant, HikariDataSource> pools;

  TenantDataSources(HikariDataSource mainDataSource, TenantRegistry registry, Environment environment) {
    pools = registry
      .tenants()
      .stream()
      .collect(
        Collectors.toUnmodifiableMap(Function.identity(), tenant -> open(mainDataSource, registry.declaration(tenant), environment))
      );
  }

  private static HikariDataSource open(HikariDataSource mainDataSource, TenantDeclaration tenant, Environment environment) {
    HikariConfig config = new HikariConfig();
    mainDataSource.copyStateTo(config);
    config.setPoolName(mainDataSource.getPoolName() + "-" + tenant.id());
    config.setSchema(tenant.schema());
    apply(config, tenant, environment);

    HikariDataSource pool = new HikariDataSource();
    config.copyStateTo(pool);
    return pool;
  }

  private static void apply(HikariConfig config, TenantDeclaration tenant, Environment environment) {
    PoolSettings pool = tenant.pool();
    DatabaseAccess database = pool.database();
    database
      .jdbcUrl()
      .ifPresent(jdbcUrl -> {
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName(driverOf(tenant, jdbcUrl));
      });
    database.username().ifPresent(config::setUsername);
    database.secretRef().ifPresent(secretRef -> config.setPassword(secret(tenant, secretRef, environment)));
    pool.maximumSize().ifPresent(config::setMaximumPoolSize);
  }

  /** Le pilote du pool principal ne reconnait pas forcement l'URL d'une base dediee. */
  private static String driverOf(TenantDeclaration tenant, String jdbcUrl) {
    try {
      return DriverManager.getDriver(jdbcUrl).getClass().getName();
    } catch (SQLException e) {
      throw new IllegalStateException("No JDBC driver for the jdbc_url of tenant " + tenant.id(), e);
    }
  }

  private static String secret(TenantDeclaration tenant, String secretRef, Environment environment) {
    String secret = environment.getProperty(secretRef);
    if (secret == null) {
      throw new IllegalStateException(
        "Tenant " + tenant.id() + ": environment variable " + secretRef + ", named by its secret_ref, is not set"
      );
    }

    return secret;
  }

  HikariDataSource dataSource(Tenant tenant) {
    return Optional.ofNullable(pools.get(tenant)).orElseThrow(NotTenantedUserException::new);
  }

  /**
   * Connexions hors pool, sur la base de l'entreprise : migrer son schema au demarrage n'ouvre pas son pool,
   * qui ne tiendra des connexions qu'a la premiere requete.
   */
  DataSource unpooled(Tenant tenant) {
    HikariDataSource pool = dataSource(tenant);
    DriverManagerDataSource dataSource = new DriverManagerDataSource(pool.getJdbcUrl(), pool.getUsername(), pool.getPassword());
    dataSource.setDriverClassName(pool.getDriverClassName());
    return dataSource;
  }

  @Override
  public void destroy() {
    pools.values().forEach(HikariDataSource::close);
  }
}
