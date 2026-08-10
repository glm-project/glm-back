package com.glm.glmback.wire.database.infrastructure.secondary;

import static com.glm.glmback.shared.multitenancy.domain.TenantFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.multitenancy.application.CurrentTenant;
import com.glm.glmback.shared.multitenancy.application.NotTenantedUserException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CurrentTenantResolverTest {

  @Mock
  private TenantSchemas tenantSchemas;

  @InjectMocks
  private CurrentTenantResolver resolver;

  @BeforeEach
  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void shouldResolveDefaultSchemaOutOfRequest() {
    when(tenantSchemas.defaultSchema()).thenReturn("public");

    assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("public");
  }

  @Test
  void shouldNotResolveTenantInRequestWithoutAuthentication() {
    inRequest();

    assertThatThrownBy(resolver::resolveCurrentTenantIdentifier).isExactlyInstanceOf(NotTenantedUserException.class);
  }

  @Test
  void shouldResolveSchemaOfAuthenticatedTenant() {
    inRequest();
    authenticateOn(TENANT_IMPECCMOLD.value());
    when(tenantSchemas.schema(TENANT_IMPECCMOLD)).thenReturn("impeccmold");

    assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo("impeccmold");
  }

  private void inRequest() {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
  }

  @Test
  void shouldValidateExistingCurrentSessions() {
    assertThat(resolver.validateExistingCurrentSessions()).isTrue();
  }

  private void authenticateOn(String tenant) {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", JwsAlgorithms.RS256).subject("seed4j").claim(CurrentTenant.TENANT, tenant).build();

    var securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    SecurityContextHolder.setContext(securityContext);
  }
}
