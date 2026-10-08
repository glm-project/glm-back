package com.glm.glmback.wire.database.infrastructure.secondary;

import static com.glm.glmback.shared.multitenancy.domain.TenantFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.multitenancy.application.NotTenantedUserException;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import com.glm.glmback.wire.database.infrastructure.secondary.TenantRegistry.TenantDeclaration;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class TenantDataSourcesTest {

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
    HikariDataSource main = new HikariDataSource();
    main.setJdbcUrl("jdbc:postgresql://localhost:5432/glmproject");
    main.setUsername("glmproject");
    main.setPoolName("Hikari");
    main.setMaximumPoolSize(3);
    main.setAutoCommit(false);

    return new TenantDataSources(
      main,
      new TenantRegistry(
        "public",
        List.of(
          new TenantDeclaration(TENANT_IMPECCMOLD.value(), "impeccmold"),
          new TenantDeclaration(TENANT_KATILYS.value(), "katilys_schema")
        )
      )
    );
  }
}
