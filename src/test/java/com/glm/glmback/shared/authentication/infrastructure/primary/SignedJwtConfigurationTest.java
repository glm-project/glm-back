package com.glm.glmback.shared.authentication.infrastructure.primary;

import static com.glm.glmback.shared.authentication.infrastructure.primary.SignedJwtFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantAuthorizationManager;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.CorsFilter;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class SignedJwtConfigurationTest {

  private final AtomicInteger userInfoCalls = new AtomicInteger();
  private final String signingKeys = new JWKSet(SIGNING_KEY.toPublicJWK()).toString();
  private HttpServer identityProvider;
  private String issuer;
  private JwtDecoder decoder;

  @BeforeEach
  void startSignedTokenDecoder() throws IOException {
    identityProvider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    issuer = "http://127.0.0.1:" + identityProvider.getAddress().getPort();
    identityProvider.createContext("/", this::identityResponse);
    identityProvider.start();

    ApplicationSecurityProperties properties = new ApplicationSecurityProperties();
    properties.getOauth2().setAudience(List.of(API_AUDIENCE));
    SecurityConfiguration configuration = new SecurityConfiguration(
      mock(CorsFilter.class),
      properties,
      mock(TenantAuthorizationManager.class)
    );
    ReflectionTestUtils.setField(configuration, "issuerUri", issuer);
    decoder = configuration.jwtDecoder();
  }

  @AfterEach
  void stopIdentityProvider() {
    RequestContextHolder.resetRequestAttributes();
    identityProvider.stop(0);
  }

  @Test
  void shouldUseSignedClaimsAcrossNewRequestsWithoutCallingUserInfo() throws Exception {
    String accessToken = token(claims(issuer).build());
    for (int request = 0; request < 1000; request++) {
      requestWith(accessToken);
      Jwt decoded = decoder.decode(accessToken);
      assertThat(decoded.getClaimAsString("tenant")).isEqualTo("impeccmold");
      assertThat(decoded.getClaimAsString("preferred_username")).isEqualTo("gestionnaire.impeccmold");
      assertThat(Claims.extractAuthorityFromClaims(decoded.getClaims()))
        .extracting(authority -> authority.getAuthority())
        .containsExactly("ROLE_GESTIONNAIRE", "ROLE_USER");
    }
    assertThat(userInfoCalls).hasValue(0);
  }

  @Test
  void shouldKeepRolesAndTenantSpecificToEachNewTokenOfTheSameSubject() throws Exception {
    String manager = token(claims(issuer).build());
    String user = token(
      claims(issuer).claim("roles", List.of("ROLE_USER")).claim("tenant", "katilys").claim("preferred_username", "user.katilys").build()
    );
    requestWith(manager);
    decoder.decode(manager);
    requestWith(user);
    Jwt decoded = decoder.decode(user);

    assertThat(decoded.getSubject()).isEqualTo("same-subject");
    assertThat(decoded.getClaimAsString("tenant")).isEqualTo("katilys");
    assertThat(decoded.getClaimAsString("preferred_username")).isEqualTo("user.katilys");
    assertThat(Claims.extractAuthorityFromClaims(decoded.getClaims()))
      .extracting(authority -> authority.getAuthority())
      .containsExactly("ROLE_USER");
    assertThat(userInfoCalls).hasValue(0);
  }

  @Test
  void shouldRejectExpiredTokensWrongIssuerAndWrongAudience() throws Exception {
    for (JWTClaimsSet invalid : List.of(
      claims(issuer).expirationTime(Date.from(Instant.now().minusSeconds(120))).build(),
      claims("https://other-issuer.invalid").build(),
      claims(issuer).audience("another-api").build()
    )) {
      String invalidToken = token(invalid);
      requestWith(invalidToken);
      assertThatThrownBy(() -> decoder.decode(invalidToken)).isInstanceOf(JwtException.class);
    }
    assertThat(userInfoCalls).hasValue(0);
  }

  @Test
  void shouldRejectTamperedSignature() throws Exception {
    String valid = token(claims(issuer).build());
    int signatureStart = valid.lastIndexOf('.') + 1;
    char changed = valid.charAt(signatureStart) == 'A' ? 'B' : 'A';
    String tampered = valid.substring(0, signatureStart) + changed + valid.substring(signatureStart + 1);
    requestWith(tampered);

    assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(JwtException.class);
    assertThat(userInfoCalls).hasValue(0);
  }

  private void requestWith(String token) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

  private void identityResponse(HttpExchange exchange) throws IOException {
    String body;
    switch (exchange.getRequestURI().getPath()) {
      case "/jwks" -> body = signingKeys;
      case "/userinfo" -> {
        userInfoCalls.incrementAndGet();
        body = "{}";
      }
      default -> body = JsonMapper.builder().build().writeValueAsString(Map.of("issuer", issuer, "jwks_uri", issuer + "/jwks"));
    }
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
