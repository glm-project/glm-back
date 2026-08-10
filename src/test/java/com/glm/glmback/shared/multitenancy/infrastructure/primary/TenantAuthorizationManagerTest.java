package com.glm.glmback.shared.multitenancy.infrastructure.primary;

import static com.glm.glmback.shared.multitenancy.domain.TenantFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import com.glm.glmback.shared.multitenancy.domain.Tenants;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@UnitTest
class TenantAuthorizationManagerTest {

  private static final Tenants TENANTS_CONNUS = tenant -> Set.of(TENANT_IMPECCMOLD, TENANT_KATILYS).contains(tenant);

  private final TenantAuthorizationManager authorizations = new TenantAuthorizationManager(TENANTS_CONNUS);

  @BeforeEach
  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldNotAuthorizeWithoutAuthentication() {
    assertThat(authorize(() -> null).isGranted()).isFalse();
  }

  @Test
  void shouldNotAuthorizeUnauthenticatedToken() {
    TestingAuthenticationToken token = new TestingAuthenticationToken("admin", "admin");
    token.setAuthenticated(false);

    assertThat(authorize(() -> token).isGranted()).isFalse();
  }

  @Test
  void shouldNotAuthorizeAnonymousUser() {
    var anonymous = new AnonymousAuthenticationToken("key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

    assertThat(authorize(() -> anonymous).isGranted()).isFalse();
  }

  @Test
  void shouldNotAuthorizeAuthenticatedUserWithoutTenant() {
    assertThat(authorize(authenticationOn(null)).isGranted()).isFalse();
  }

  @Test
  void shouldNotAuthorizeAuthenticatedUserOfUnknownTenant() {
    assertThat(authorize(authenticationOn(new Tenant("inconnu"))).isGranted()).isFalse();
  }

  @Test
  void shouldAuthorizeAuthenticatedUserOfKnownTenant() {
    assertThat(authorize(authenticationOn(TENANT_IMPECCMOLD)).isGranted()).isTrue();
  }

  private AuthorizationResult authorize(Supplier<Authentication> authentication) {
    return authorizations.authorize(authentication, null);
  }

  private Supplier<Authentication> authenticationOn(Tenant tenant) {
    Jwt.Builder jwt = Jwt.withTokenValue("token").header("alg", JwsAlgorithms.RS256).subject("admin");
    if (tenant != null) {
      jwt.claim("tenant", tenant.value());
    }

    var token = new JwtAuthenticationToken(jwt.build(), List.of(new SimpleGrantedAuthority("ROLE_USER")));
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(token);
    SecurityContextHolder.setContext(context);

    return () -> token;
  }
}
