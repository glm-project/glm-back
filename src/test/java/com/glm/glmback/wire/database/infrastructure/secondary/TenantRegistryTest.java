package com.glm.glmback.wire.database.infrastructure.secondary;

import static com.glm.glmback.shared.multitenancy.domain.TenantFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringNotMatchingPatternException;
import com.glm.glmback.shared.multitenancy.application.NotTenantedUserException;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class TenantRegistryTest {

  @Test
  void shouldNotBuildWithoutSchema() {
    List<TenantDeclaration> tenants = List.of(new TenantDeclaration("impeccmold", null));

    assertThatThrownBy(() -> new TenantRegistry("public", tenants))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("schema of tenant impeccmold");
  }

  @Test
  void shouldNotBuildWithSchemaOutOfPattern() {
    List<TenantDeclaration> tenants = List.of(new TenantDeclaration("impeccmold", "Impecc Mold"));

    assertThatThrownBy(() -> new TenantRegistry("public", tenants))
      .isExactlyInstanceOf(StringNotMatchingPatternException.class)
      .hasMessageContaining("schema of tenant impeccmold");
  }

  @Test
  void shouldNotBuildWithTenantOutOfPattern() {
    List<TenantDeclaration> tenants = List.of(new TenantDeclaration("Impecc Mold", "impeccmold"));

    assertThatThrownBy(() -> new TenantRegistry("public", tenants))
      .isExactlyInstanceOf(StringNotMatchingPatternException.class)
      .hasMessageContaining("tenant");
  }

  @Test
  void shouldGetSchemaOfDeclaredTenant() {
    assertThat(impeccmoldEtKatilys().schema(TENANT_KATILYS)).isEqualTo("katilys_schema");
  }

  @Test
  void shouldNotGetSchemaOfUnknownTenant() {
    TenantRegistry registry = impeccmoldEtKatilys();
    Tenant inconnu = new Tenant("inconnu");

    assertThatThrownBy(() -> registry.schema(inconnu)).isExactlyInstanceOf(NotTenantedUserException.class);
  }

  @Test
  void shouldGiveTheDeclarationOfATenant() {
    assertThat(impeccmoldEtKatilys().declaration(TENANT_KATILYS)).isEqualTo(new TenantDeclaration("katilys", "katilys_schema"));
  }

  @Test
  void shouldNotGiveTheDeclarationOfAnUnknownTenant() {
    TenantRegistry registry = impeccmoldEtKatilys();
    Tenant inconnu = new Tenant("inconnu");

    assertThatThrownBy(() -> registry.declaration(inconnu)).isExactlyInstanceOf(NotTenantedUserException.class);
  }

  @Test
  void shouldKnowDeclaredTenants() {
    assertThat(impeccmoldEtKatilys().contains(TENANT_IMPECCMOLD)).isTrue();
    assertThat(impeccmoldEtKatilys().contains(new Tenant("inconnu"))).isFalse();
  }

  @Test
  void shouldListDeclaredTenants() {
    assertThat(impeccmoldEtKatilys().tenants()).containsExactlyInAnyOrder(TENANT_IMPECCMOLD, TENANT_KATILYS);
  }

  @Test
  void shouldMapTenantIdentifierToItsSchema() {
    assertThat(impeccmoldEtKatilys().schemaName("katilys")).isEqualTo("katilys_schema");
  }

  @Test
  void shouldMapOutOfRequestIdentifierToDefaultSchema() {
    assertThat(impeccmoldEtKatilys().schemaName(CurrentTenantResolver.OUT_OF_REQUEST)).isEqualTo("public");
  }

  @Test
  void shouldNotMapUnknownTenantIdentifier() {
    TenantRegistry registry = impeccmoldEtKatilys();

    assertThatThrownBy(() -> registry.schemaName("inconnu")).isExactlyInstanceOf(NotTenantedUserException.class);
  }

  @Test
  void shouldKeepDefaultSchemaOutOfRequest() {
    assertThat(impeccmoldEtKatilys().defaultSchema()).isEqualTo("public");
  }

  private static TenantRegistry impeccmoldEtKatilys() {
    return new TenantRegistry(
      "public",
      List.of(
        new TenantDeclaration(TENANT_IMPECCMOLD.value(), "impeccmold"),
        new TenantDeclaration(TENANT_KATILYS.value(), "katilys_schema")
      )
    );
  }
}
