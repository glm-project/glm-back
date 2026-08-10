package com.glm.glmback.shared.multitenancy.domain;

import static com.glm.glmback.shared.multitenancy.domain.TenantFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringNotMatchingPatternException;
import org.junit.jupiter.api.Test;

@UnitTest
class TenantTest {

  @Test
  void shouldNotBuildWithoutTenant() {
    assertThatThrownBy(() -> new Tenant(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("tenant");
  }

  @Test
  void shouldNotBuildWithBlankTenant() {
    assertThatThrownBy(() -> new Tenant(" "))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("tenant");
  }

  @Test
  void shouldNotBuildWithTenantOutOfPattern() {
    assertThatThrownBy(() -> new Tenant("Impecc Mold"))
      .isExactlyInstanceOf(StringNotMatchingPatternException.class)
      .hasMessageContaining("tenant");
  }

  @Test
  void shouldBuildTenantFromValue() {
    assertThat(new Tenant("impeccmold").value()).isEqualTo("impeccmold");
  }

  @Test
  void shouldNotBuildOptionalTenantFromNullValue() {
    assertThat(Tenant.of(null)).isEmpty();
  }

  @Test
  void shouldNotBuildOptionalTenantFromBlankValue() {
    assertThat(Tenant.of(" ")).isEmpty();
  }

  @Test
  void shouldBuildOptionalTenantFromValue() {
    assertThat(Tenant.of("katilys")).contains(TENANT_KATILYS);
  }
}
