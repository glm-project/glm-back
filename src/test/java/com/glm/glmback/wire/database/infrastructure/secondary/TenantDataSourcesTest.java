package com.glm.glmback.wire.database.infrastructure.secondary;

import static com.glm.glmback.shared.multitenancy.domain.TenantFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.multitenancy.application.NotTenantedUserException;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import com.glm.glmback.wire.database.infrastructure.secondary.TenantDeclaration.DatabaseAccess;
import com.glm.glmback.wire.database.infrastructure.secondary.TenantDeclaration.PoolSettings;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;

@UnitTest
class TenantDataSourcesTest {

  private static final TenantDeclaration ACME_SUR_BASE_DEDIEE = new TenantDeclaration(
    "acme",
    "acme",
    new PoolSettings(
      new DatabaseAccess(
        Optional.of("jdbc:postgresql://pg-acme:5432/acme"),
        Optional.of("acme"),
        Optional.of("GLM_TENANT_ACME_DB_PASSWORD")
      ),
      Optional.of(7)
    )
  );

  @Test
  void shouldGiveEachTenantItsOwnPool() {
    TenantDataSources pools = impeccmoldEtKatilys();

    assertThat(pools.dataSource(TENANT_IMPECCMOLD)).isNotSameAs(pools.dataSource(TENANT_KATILYS));
  }

  @Test
  void shouldInheritMainPoolConfiguration() {
    HikariDataSource pool = impeccmoldEtKatilys().dataSource(TENANT_KATILYS);

    assertThat(pool.getJdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/glmproject");
    assertThat(pool.getUsername()).isEqualTo("glmproject");
    assertThat(pool.getPassword()).isEqualTo("main-password");
    assertThat(pool.getMaximumPoolSize()).isEqualTo(3);
    assertThat(pool.isAutoCommit()).isFalse();
  }

  @Test
  void shouldNameEachPoolAfterItsTenant() {
    assertThat(impeccmoldEtKatilys().dataSource(TENANT_KATILYS).getPoolName()).isEqualTo("Hikari-katilys");
  }

  @Test
  void shouldPositionEachPoolOnItsTenantSchema() {
    assertThat(impeccmoldEtKatilys().dataSource(TENANT_KATILYS).getSchema()).isEqualTo("katilys_schema");
  }

  @Test
  void shouldPointThePoolOfATenantToItsDedicatedDatabase() {
    HikariDataSource pool = acme(new MockEnvironment().withProperty("GLM_TENANT_ACME_DB_PASSWORD", "acme-password")).dataSource(
      new Tenant("acme")
    );

    assertThat(pool.getJdbcUrl()).isEqualTo("jdbc:postgresql://pg-acme:5432/acme");
    assertThat(pool.getDriverClassName()).isEqualTo("org.postgresql.Driver");
    assertThat(pool.getUsername()).isEqualTo("acme");
    assertThat(pool.getPassword()).isEqualTo("acme-password");
    assertThat(pool.getMaximumPoolSize()).isEqualTo(7);
  }

  @Test
  void shouldNotStartWhenTheSecretOfATenantIsMissing() {
    MockEnvironment environment = new MockEnvironment();

    assertThatThrownBy(() -> acme(environment))
      .isExactlyInstanceOf(IllegalStateException.class)
      .hasMessageContaining("acme")
      .hasMessageContaining("GLM_TENANT_ACME_DB_PASSWORD");
  }

  @Test
  void shouldNotStartWithoutADriverForTheDedicatedDatabase() {
    TenantDeclaration inconnue = new TenantDeclaration(
      "inconnue",
      "inconnue",
      new PoolSettings(new DatabaseAccess(Optional.of("jdbc:unknown://somewhere"), Optional.empty(), Optional.empty()), Optional.empty())
    );
    HikariDataSource main = main();
    TenantRegistry registry = new TenantRegistry("public", List.of(inconnue));
    MockEnvironment environment = new MockEnvironment();

    assertThatThrownBy(() -> new TenantDataSources(main, registry, environment))
      .isExactlyInstanceOf(IllegalStateException.class)
      .hasMessageContaining("inconnue");
  }

  @Test
  void shouldGiveUnpooledConnectionsToTheTenantDatabase() {
    DriverManagerDataSource unpooled = (DriverManagerDataSource) acme(
      new MockEnvironment().withProperty("GLM_TENANT_ACME_DB_PASSWORD", "acme-password")
    ).unpooled(new Tenant("acme"));

    assertThat(unpooled.getUrl()).isEqualTo("jdbc:postgresql://pg-acme:5432/acme");
    assertThat(unpooled.getUsername()).isEqualTo("acme");
    assertThat(unpooled.getPassword()).isEqualTo("acme-password");
  }

  @Test
  void shouldNotGiveAPoolToAnUnknownTenant() {
    TenantDataSources pools = impeccmoldEtKatilys();
    Tenant inconnu = new Tenant("inconnu");

    assertThatThrownBy(() -> pools.dataSource(inconnu)).isExactlyInstanceOf(NotTenantedUserException.class);
  }

  @Test
  void shouldCloseEveryPoolOnDestroy() {
    TenantDataSources pools = impeccmoldEtKatilys();

    pools.destroy();

    assertThat(pools.dataSource(TENANT_IMPECCMOLD).isClosed()).isTrue();
    assertThat(pools.dataSource(TENANT_KATILYS).isClosed()).isTrue();
  }

  private static TenantDataSources impeccmoldEtKatilys() {
    return new TenantDataSources(
      main(),
      new TenantRegistry(
        "public",
        List.of(
          new TenantDeclaration(TENANT_IMPECCMOLD.value(), "impeccmold"),
          new TenantDeclaration(TENANT_KATILYS.value(), "katilys_schema")
        )
      ),
      new MockEnvironment()
    );
  }

  private static TenantDataSources acme(MockEnvironment environment) {
    return new TenantDataSources(main(), new TenantRegistry("public", List.of(ACME_SUR_BASE_DEDIEE)), environment);
  }

  private static HikariDataSource main() {
    HikariDataSource main = new HikariDataSource();
    main.setJdbcUrl("jdbc:postgresql://localhost:5432/glmproject");
    main.setDriverClassName("org.postgresql.Driver");
    main.setUsername("glmproject");
    main.setPassword("main-password");
    main.setPoolName("Hikari");
    main.setMaximumPoolSize(3);
    main.setAutoCommit(false);
    return main;
  }
}
