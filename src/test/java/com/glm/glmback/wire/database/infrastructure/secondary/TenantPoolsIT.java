package com.glm.glmback.wire.database.infrastructure.secondary;

import static com.glm.glmback.shared.multitenancy.domain.TenantFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@IntegrationTest
class TenantPoolsIT {

  @Autowired
  private TenantDataSources pools;

  @Autowired
  private HikariDataSource mainDataSource;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private EntityManager entityManager;

  @AfterEach
  void cleanup() {
    RequestContextHolder.resetRequestAttributes();
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldTakeTheConnectionOfATenantFromItsOwnPoolOnItsSchema() {
    onTenant(TENANT_IMPECCMOLD, () -> {
      String schema = transactions.execute(transaction -> {
        String current = (String) entityManager.createNativeQuery("SELECT current_schema()").getSingleResult();
        assertThat(activeConnections(pools.dataSource(TENANT_IMPECCMOLD))).isEqualTo(1);
        assertThat(activeConnections(pools.dataSource(TENANT_KATILYS))).isZero();
        assertThat(activeConnections(mainDataSource)).isZero();
        return current;
      });

      assertThat(schema).isEqualTo("impeccmold");
      return null;
    });
  }

  @Test
  void shouldServeATenantWhileAnotherHasExhaustedItsPool() throws Exception {
    HikariDataSource impeccmold = pools.dataSource(TENANT_IMPECCMOLD);
    List<Connection> held = new ArrayList<>();
    try {
      for (int index = 0; index < impeccmold.getMaximumPoolSize(); index++) {
        held.add(impeccmold.getConnection());
      }

      String schema = onTenant(TENANT_KATILYS, () ->
        transactions.execute(transaction -> (String) entityManager.createNativeQuery("SELECT current_schema()").getSingleResult())
      );

      assertThat(schema).isEqualTo("katilys");
    } finally {
      for (Connection connection : held) {
        connection.close();
      }
    }
  }

  private static int activeConnections(HikariDataSource dataSource) {
    return Optional.ofNullable(dataSource.getHikariPoolMXBean()).map(HikariPoolMXBean::getActiveConnections).orElse(0);
  }

  private static <T> T onTenant(Tenant tenant, Supplier<T> action) {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    TenantSecurityContexts.authenticateOn(tenant.value());
    return action.get();
  }
}
