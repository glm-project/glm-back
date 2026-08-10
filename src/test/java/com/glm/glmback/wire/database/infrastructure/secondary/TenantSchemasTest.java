package com.glm.glmback.wire.database.infrastructure.secondary;

import static com.glm.glmback.shared.multitenancy.domain.TenantFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringNotMatchingPatternException;
import com.glm.glmback.shared.multitenancy.application.NotTenantedUserException;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import com.glm.glmback.wire.database.infrastructure.secondary.MultitenancyProperties.TenantProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class TenantSchemasTest {

  @Test
  void shouldNotBuildWithoutSchema() {
    MultitenancyProperties properties = properties("impeccmold", null);

    assertThatThrownBy(() -> new TenantSchemas(properties))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("schema");
  }

  @Test
  void shouldNotBuildWithSchemaOutOfPattern() {
    MultitenancyProperties properties = properties("impeccmold", "Impecc Mold");

    assertThatThrownBy(() -> new TenantSchemas(properties))
      .isExactlyInstanceOf(StringNotMatchingPatternException.class)
      .hasMessageContaining("schema");
  }

  @Test
  void shouldGetSchemaOfConfiguredTenant() {
    assertThat(impeccmoldEtKatilys().schema(TENANT_KATILYS)).isEqualTo("katilys_schema");
  }

  @Test
  void shouldNotGetSchemaOfUnknownTenant() {
    TenantSchemas schemas = impeccmoldEtKatilys();
    Tenant inconnu = new Tenant("inconnu");

    assertThatThrownBy(() -> schemas.schema(inconnu)).isExactlyInstanceOf(NotTenantedUserException.class);
  }

  @Test
  void shouldListAllConfiguredSchemas() {
    assertThat(impeccmoldEtKatilys().schemas()).containsExactlyInAnyOrder("impeccmold", "katilys_schema");
  }

  @Test
  void shouldKnownConfiguredTenants() {
    assertThat(impeccmoldEtKatilys().contains(TENANT_IMPECCMOLD)).isTrue();
    assertThat(impeccmoldEtKatilys().contains(new Tenant("inconnu"))).isFalse();
  }

  @Test
  void shouldDefaultToPublicSchemaOutOfRequest() {
    assertThat(impeccmoldEtKatilys().defaultSchema()).isEqualTo("public");
  }

  @Test
  void shouldNotBuildWithDefaultSchemaOutOfPattern() {
    MultitenancyProperties properties = properties(TENANT_IMPECCMOLD.value(), "impeccmold");
    properties.setDefaultSchema("Public Schema");

    assertThatThrownBy(() -> new TenantSchemas(properties))
      .isExactlyInstanceOf(StringNotMatchingPatternException.class)
      .hasMessageContaining("default-schema");
  }

  private static TenantSchemas impeccmoldEtKatilys() {
    MultitenancyProperties properties = new MultitenancyProperties();
    properties.setTenants(List.of(tenant(TENANT_IMPECCMOLD.value(), "impeccmold"), tenant(TENANT_KATILYS.value(), "katilys_schema")));

    return new TenantSchemas(properties);
  }

  private static MultitenancyProperties properties(String id, String schema) {
    MultitenancyProperties properties = new MultitenancyProperties();
    properties.setTenants(List.of(tenant(id, schema)));

    return properties;
  }

  private static TenantProperties tenant(String id, String schema) {
    TenantProperties tenant = new TenantProperties();
    tenant.setId(id);
    tenant.setSchema(schema);

    return tenant;
  }
}
