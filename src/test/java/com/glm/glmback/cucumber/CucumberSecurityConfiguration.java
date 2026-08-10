package com.glm.glmback.cucumber;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.shared.authentication.infrastructure.primary.TestSecurityConfiguration;
import com.glm.glmback.shared.multitenancy.application.CurrentTenant;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Allows Cucumber scenarios to authenticate with a fake bearer token (built by {@code CucumberAuthenticationSteps}) instead of a real
 * Keycloak token, while still exercising the real {@code JwtAuthenticationConverter}/{@code JwtGrantedAuthorityConverter} chain.
 */
@TestConfiguration
@Import(TestSecurityConfiguration.class)
public class CucumberSecurityConfiguration {

  @Bean
  @Primary
  JwtDecoder cucumberJwtDecoder() {
    JwtDecoder decoder = mock(JwtDecoder.class);
    when(decoder.decode(anyString())).thenAnswer(invocation -> buildJwt(invocation.getArgument(0)));

    return decoder;
  }

  private Jwt buildJwt(String token) {
    String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
    String[] parts = decoded.split("\\|", 3);
    String username = parts[0];
    List<String> roles = parts.length > 1 ? List.of(parts[1].split(",")) : List.of();

    Jwt.Builder jwt = Jwt.withTokenValue(token)
      .header("alg", "none")
      .claim("sub", username)
      .claim("preferred_username", username)
      .claim("groups", roles)
      .issuedAt(Instant.now())
      .expiresAt(Instant.now().plusSeconds(3600));

    if (parts.length > 2 && !parts[2].isBlank()) {
      jwt.claim(CurrentTenant.TENANT, parts[2]);
    }

    return jwt.build();
  }
}
