package com.glm.glmback.wire.database.infrastructure.secondary;

import static com.glm.glmback.shared.multitenancy.domain.TenantFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@IntegrationTest
class TenantRegistryIT {

  @Autowired
  private TenantRegistry registry;

  @Autowired
  private DataSource dataSource;

  @Test
  void shouldKnowActiveTenantsOfTheTable() {
    assertThat(registry.contains(TENANT_IMPECCMOLD)).isTrue();
    assertThat(registry.schema(TENANT_KATILYS)).isEqualTo("katilys");
  }

  @Test
  void shouldIgnoreASuspendedTenantAndLeaveItsSchemaUntouched() {
    assertThat(registry.contains(new Tenant("entreprise_suspendue"))).isFalse();
    assertThat(
      JdbcClient.create(dataSource)
        .sql("SELECT nspname FROM pg_namespace WHERE nspname = 'entreprise_suspendue'")
        .query(String.class)
        .list()
    ).isEmpty();
  }
}
