package com.glm.glmback.wire.database.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@IntegrationTest
class AdminSchemaIT {

  @Autowired
  private DataSource dataSource;

  @Test
  void shouldSeedTestTenantsInTheDefaultSchema() {
    assertThat(jdbc().sql("SELECT id || ':' || schema_name || ':' || status FROM public.tenant").query(String.class).list()).contains(
      "impeccmold:impeccmold:ACTIVE",
      "katilys:katilys:ACTIVE"
    );
  }

  @Test
  void shouldRejectATenantOutOfPattern() {
    JdbcClient.StatementSpec insert = jdbc().sql(
      "INSERT INTO public.tenant (id, schema_name, status) VALUES ('Acme Corp', 'acme', 'ACTIVE')"
    );

    assertThatThrownBy(insert::update).isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ck_tenant_id");
  }

  @Test
  void shouldRejectAnUnknownStatus() {
    JdbcClient.StatementSpec insert = jdbc().sql("INSERT INTO public.tenant (id, schema_name, status) VALUES ('acme', 'acme', 'ARCHIVED')");

    assertThatThrownBy(insert::update).isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("ck_tenant_status");
  }

  private JdbcClient jdbc() {
    return JdbcClient.create(dataSource);
  }
}
