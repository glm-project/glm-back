package com.glm.glmback.shared.multitenancy.application;

import static com.glm.glmback.shared.multitenancy.application.CurrentTenant.*;
import static com.glm.glmback.shared.multitenancy.domain.TenantFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@UnitTest
class CurrentTenantTest {

  @BeforeEach
  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldNotGetTenantWithoutAuthentication() {
    assertThatThrownBy(CurrentTenant::tenant).isExactlyInstanceOf(NotTenantedUserException.class);
  }

  @Test
  void shouldNotGetOptionalTenantWithoutAuthentication() {
    assertThat(optionalTenant()).isEmpty();
  }

  @Test
  void shouldNotGetTenantFromJwtWithoutTenantClaim() {
    authenticate(new JwtAuthenticationToken(jwt(Map.of("preferred_username", "admin")), userAuthorities()));

    assertThatThrownBy(CurrentTenant::tenant).isExactlyInstanceOf(NotTenantedUserException.class);
  }

  @Test
  void shouldGetTenantFromJwtClaim() {
    authenticate(new JwtAuthenticationToken(jwt(Map.of(TENANT, "impeccmold")), userAuthorities()));

    assertThat(tenant()).isEqualTo(TENANT_IMPECCMOLD);
  }

  @Test
  void shouldGetTenantFromOidcUserAttribute() {
    authenticate(oAuth2AuthenticationToken(Map.of("sub", 123, TENANT, "katilys")));

    assertThat(optionalTenant()).contains(TENANT_KATILYS);
  }

  @Test
  void shouldNotGetTenantFromOidcUserWithoutTenantAttribute() {
    authenticate(oAuth2AuthenticationToken(Map.of("sub", 123)));

    assertThat(optionalTenant()).isEmpty();
  }

  @Test
  void shouldNotGetTenantFromUnknownAuthentication() {
    authenticate(new UsernamePasswordAuthenticationToken("admin", "admin", userAuthorities()));

    assertThat(optionalTenant()).isEmpty();
  }

  private static Jwt jwt(Map<String, Object> claims) {
    Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", JwsAlgorithms.RS256).subject("seed4j");
    claims.forEach(builder::claim);

    return builder.build();
  }

  private static OAuth2AuthenticationToken oAuth2AuthenticationToken(Map<String, Object> claims) {
    OidcIdToken idToken = new OidcIdToken(ID_TOKEN, Instant.now(), Instant.now().plusSeconds(60), claims);
    Collection<GrantedAuthority> authorities = userAuthorities();
    OidcUser user = new DefaultOidcUser(authorities, idToken);

    return new OAuth2AuthenticationToken(user, authorities, "oidc");
  }

  private static Collection<GrantedAuthority> userAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_USER"));
  }

  private void authenticate(Authentication token) {
    var securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(token);
    SecurityContextHolder.setContext(securityContext);
  }
}
